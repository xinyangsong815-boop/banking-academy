package cn.academy.bank.customer.infrastructure;

import cn.academy.bank.customer.domain.Customer;
import cn.academy.bank.customer.domain.CustomerRepository;
import cn.academy.bank.customer.domain.RiskLevel;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCustomerRepository implements CustomerRepository {
    private final JdbcClient jdbc;
    private final Clock clock;

    public JdbcCustomerRepository(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Optional<Customer> find(long id) {
        return jdbc.sql("SELECT * FROM customer WHERE id=:id")
                .param("id", id)
                .query(Customer.class)
                .optional();
    }

    @Override
    public List<Customer> search(String keyword, int offset, int size) {
        return jdbc.sql(
                        "SELECT * FROM customer WHERE LOWER(name) LIKE :q OR phone LIKE :q ORDER BY id DESC LIMIT :size OFFSET :offset")
                .param("q", pattern(keyword))
                .param("size", size)
                .param("offset", offset)
                .query(Customer.class)
                .list();
    }

    @Override
    public long count(String keyword) {
        return jdbc.sql("SELECT COUNT(*) FROM customer WHERE LOWER(name) LIKE :q OR phone LIKE :q")
                .param("q", pattern(keyword))
                .query(Long.class)
                .single();
    }

    @Override
    public long save(String name, String phone, String email, RiskLevel riskLevel) {
        var keys = new GeneratedKeyHolder();
        jdbc.sql(
                        "INSERT INTO customer(name,phone,email,risk_level,created_at) VALUES (:name,:phone,:email,:risk,:now)")
                .param("name", name)
                .param("phone", phone)
                .param("email", email)
                .param("risk", riskLevel.name())
                .param("now", OffsetDateTime.now(clock))
                .update(keys, "id");
        return keys.getKey().longValue();
    }

    @Override
    public void update(long id, String name, String phone, String email, RiskLevel riskLevel) {
        jdbc.sql(
                        "UPDATE customer SET name=:name,phone=:phone,email=:email,risk_level=:risk WHERE id=:id")
                .param("name", name)
                .param("phone", phone)
                .param("email", email)
                .param("risk", riskLevel.name())
                .param("id", id)
                .update();
    }

    private String pattern(String keyword) {
        // 参数绑定阻止 SQL 注入；本例允许 % 和 _ 作为搜索通配符。
        return "%" + keyword.strip().toLowerCase(java.util.Locale.ROOT) + "%";
    }
}
