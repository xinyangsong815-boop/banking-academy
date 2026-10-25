package cn.academy.bank.ledger.application;

import cn.academy.bank.account.domain.Account;
import cn.academy.bank.account.domain.AccountRepository;
import cn.academy.bank.ledger.domain.PostingLine;
import cn.academy.bank.shared.domain.BusinessException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 资金写入的统一入口；调用方必须先建立事务，再按统一顺序获取全部账户锁。 */
@Service
public class PostingService {
    private final AccountRepository accounts;
    private final JdbcClient jdbc;
    private final Clock clock;

    public PostingService(AccountRepository accounts, JdbcClient jdbc, Clock clock) {
        this.accounts = accounts;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<String, Account> lockAccounts(Collection<String> ids) {
        Map<String, Account> locked = new LinkedHashMap<>();
        ids.stream().distinct().sorted().forEach(id -> locked.put(id, accounts.lock(id)));
        return Collections.unmodifiableMap(locked);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String post(
            String businessKey,
            String kind,
            String description,
            List<PostingLine> lines,
            Map<String, Account> locked) {
        BusinessException.require(lines.size() >= 2, "UNBALANCED_BOOKING", "分录至少有两项");
        BigDecimal total =
                lines.stream().map(PostingLine::change).reduce(BigDecimal.ZERO, BigDecimal::add);
        BusinessException.require(total.signum() == 0, "UNBALANCED_BOOKING", "记账分录不平衡");
        Set<String> seen = new HashSet<>();
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        for (PostingLine line : lines) {
            BusinessException.require(
                    line.change().signum() != 0 && seen.add(line.accountId()),
                    "INVALID_BOOKING",
                    "分录不能为零或重复引用同一账户");
            Account account = Objects.requireNonNull(locked.get(line.accountId()), "账户必须先加锁");
            balances.put(line.accountId(), account.balanceAfter(line.change()));
        }
        String id = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now(clock);
        jdbc.sql(
                        "INSERT INTO booking(id,business_key,kind,description,created_at) VALUES (:id,:key,:kind,:description,:now)")
                .param("id", id)
                .param("key", businessKey)
                .param("kind", kind)
                .param("description", description)
                .param("now", now)
                .update();
        for (PostingLine line : lines) {
            BigDecimal after = balances.get(line.accountId());
            accounts.updateBalance(line.accountId(), after);
            jdbc.sql(
                            "INSERT INTO ledger_entry(booking_id,account_id,amount,balance_after,created_at) VALUES (:booking,:account,:amount,:balance,:now)")
                    .param("booking", id)
                    .param("account", line.accountId())
                    .param("amount", line.change())
                    .param("balance", after)
                    .param("now", now)
                    .update();
        }
        return id;
    }
}
