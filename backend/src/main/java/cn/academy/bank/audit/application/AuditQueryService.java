package cn.academy.bank.audit.application;

import cn.academy.bank.shared.api.PageResult;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AuditQueryService {
    private final JdbcClient jdbc;

    public AuditQueryService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public record Audit(
            long id, String actor, String action, String traceId, OffsetDateTime createdAt) {}

    @PreAuthorize("hasAuthority('audit:read')")
    public PageResult<Audit> list(int page, int size) {
        int offset = PageResult.offset(page, size);
        var items =
                jdbc.sql("SELECT * FROM audit_log ORDER BY id DESC LIMIT :size OFFSET :offset")
                        .param("size", size)
                        .param("offset", offset)
                        .query(Audit.class)
                        .list();
        return new PageResult<>(
                items,
                jdbc.sql("SELECT COUNT(*) FROM audit_log").query(Long.class).single(),
                page,
                size);
    }
}
