package cn.academy.bank.shared.infrastructure;

import cn.academy.bank.shared.domain.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 数据库唯一约束负责跨线程互斥，事务负责业务写入与响应快照原子提交。 */
@Service
public class IdempotencyService {
    private final JdbcClient jdbc;
    private final ObjectMapper mapper;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public IdempotencyService(
            JdbcClient jdbc, ObjectMapper mapper, PlatformTransactionManager manager, Clock clock) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(manager);
        this.clock = clock;
    }

    public <T> T execute(
            String scope, String key, Object payload, Class<T> type, Supplier<T> operation) {
        BusinessException.require(
                key != null && key.matches("[a-zA-Z0-9_-]{8,64}"),
                "INVALID_KEY",
                "Idempotency-Key 应为 8~64 位字母、数字、下划线或连字符");
        String hash = Hashing.sha256(json(payload));
        try {
            return transaction.execute(
                    status -> {
                        jdbc.sql(
                                        "INSERT INTO idempotent_request(scope,request_key,payload_hash,created_at) VALUES (:scope,:key,:hash,:now)")
                                .param("scope", scope)
                                .param("key", key)
                                .param("hash", hash)
                                .param("now", OffsetDateTime.now(clock))
                                .update();
                        T result = operation.get();
                        jdbc.sql(
                                        "UPDATE idempotent_request SET response_json=:response WHERE scope=:scope AND request_key=:key")
                                .param("response", json(result))
                                .param("scope", scope)
                                .param("key", key)
                                .update();
                        return result;
                    });
        } catch (DuplicateKeyException exception) {
            // 必须在失败事务退出后查询，PostgreSQL 不允许继续使用已中止的事务。
            var saved =
                    jdbc.sql(
                                    "SELECT payload_hash,response_json FROM idempotent_request WHERE scope=:scope AND request_key=:key")
                            .param("scope", scope)
                            .param("key", key)
                            .query(SavedResponse.class)
                            .optional();
            if (saved.isEmpty()) {
                throw exception;
            }
            BusinessException.require(
                    saved.get().payloadHash().equals(hash),
                    "IDEMPOTENCY_CONFLICT",
                    "同一幂等键不能用于不同请求");
            try {
                return mapper.readValue(saved.get().responseJson(), type);
            } catch (JsonProcessingException failure) {
                throw new IllegalStateException("幂等响应无法反序列化", failure);
            }
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("请求或响应无法序列化", exception);
        }
    }

    public record SavedResponse(String payloadHash, String responseJson) {}
}
