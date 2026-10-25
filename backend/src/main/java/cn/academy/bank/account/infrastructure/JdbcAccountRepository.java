package cn.academy.bank.account.infrastructure;

import cn.academy.bank.account.domain.Account;
import cn.academy.bank.account.domain.AccountRepository;
import cn.academy.bank.account.domain.AccountStatus;
import cn.academy.bank.shared.domain.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAccountRepository implements AccountRepository {
    private final JdbcClient jdbc;

    public JdbcAccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Account> find(String id) {
        return jdbc.sql("SELECT * FROM bank_account WHERE id=:id")
                .param("id", id)
                .query(Account.class)
                .optional();
    }

    @Override
    public Account lock(String id) {
        return jdbc.sql("SELECT * FROM bank_account WHERE id=:id FOR UPDATE")
                .param("id", id)
                .query(Account.class)
                .optional()
                .orElseThrow(() -> BusinessException.notFound("账户"));
    }

    @Override
    public List<Account> search(Long customerId, int offset, int size) {
        String filter = customerId == null ? "" : " AND customer_id=:customer";
        var statement =
                jdbc.sql(
                                "SELECT * FROM bank_account WHERE internal_account=FALSE"
                                        + filter
                                        + " ORDER BY created_at DESC,id LIMIT :size OFFSET :offset")
                        .param("size", size)
                        .param("offset", offset);
        if (customerId != null) {
            statement.param("customer", customerId);
        }
        return statement.query(Account.class).list();
    }

    @Override
    public long count(Long customerId) {
        var query =
                jdbc.sql(
                        "SELECT COUNT(*) FROM bank_account WHERE internal_account=FALSE"
                                + (customerId == null ? "" : " AND customer_id=:customer"));
        if (customerId != null) {
            query.param("customer", customerId);
        }
        return query.query(Long.class).single();
    }

    @Override
    public void create(Account account) {
        jdbc.sql(
                        "INSERT INTO bank_account(id,account_no,customer_id,name,balance,status,internal_account,created_at) VALUES (:id,:no,:customer,:name,:balance,:status,:internal,:now)")
                .param("id", account.id())
                .param("no", account.accountNo())
                .param("customer", account.customerId())
                .param("name", account.name())
                .param("balance", account.balance())
                .param("status", account.status().name())
                .param("internal", account.internalAccount())
                .param("now", account.createdAt())
                .update();
    }

    @Override
    public void updateBalance(String id, BigDecimal balance) {
        jdbc.sql("UPDATE bank_account SET balance=:balance WHERE id=:id")
                .param("balance", balance)
                .param("id", id)
                .update();
    }

    @Override
    public void updateStatus(String id, AccountStatus status) {
        jdbc.sql("UPDATE bank_account SET status=:status WHERE id=:id")
                .param("status", status.name())
                .param("id", id)
                .update();
    }
}
