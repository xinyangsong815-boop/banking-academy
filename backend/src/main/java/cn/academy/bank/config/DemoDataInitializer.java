package cn.academy.bank.config;

import cn.academy.bank.account.domain.Account;
import cn.academy.bank.account.domain.AccountRepository;
import cn.academy.bank.account.domain.AccountStatus;
import cn.academy.bank.customer.domain.CustomerRepository;
import cn.academy.bank.customer.domain.RiskLevel;
import cn.academy.bank.ledger.application.PostingService;
import cn.academy.bank.ledger.domain.PostingLine;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** 仅 demo profile 启用。演示余额也必须通过成组分录建立。 */
@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {
    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final PostingService posting;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public DemoDataInitializer(
            JdbcClient jdbc,
            PasswordEncoder encoder,
            CustomerRepository customers,
            AccountRepository accounts,
            PostingService posting,
            TransactionTemplate transaction,
            Clock clock) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.customers = customers;
        this.accounts = accounts;
        this.posting = posting;
        this.transaction = transaction;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (jdbc.sql("SELECT COUNT(*) FROM staff_user").query(Long.class).single() > 0) {
            return;
        }
        transaction.executeWithoutResult(
                status -> {
                    staff("admin", "教学管理员", "ADMIN");
                    staff("teller", "柜员小林", "TELLER");
                    staff("auditor", "审计员小周", "AUDITOR");
                    long first =
                            customers.save(
                                    "张晨", "13800000001", "zhang@example.test", RiskLevel.HIGH);
                    long second =
                            customers.save(
                                    "李悦", "13800000002", "li@example.test", RiskLevel.MEDIUM);
                    long third =
                            customers.save("王宁", "13800000003", "wang@example.test", RiskLevel.LOW);
                    account("SYS-CLEARING", null, "教学清算账户", true);
                    account("SYS-FEE", null, "手续费收入账户", true);
                    account("SYS-PRODUCT", null, "产品归集账户", true);
                    account("A1001", first, "张晨 · 日常账户", false);
                    account("A1002", second, "李悦 · 生活账户", false);
                    account("A1003", third, "王宁 · 储蓄账户", false);
                    var locked =
                            posting.lockAccounts(
                                    List.of("SYS-CLEARING", "A1001", "A1002", "A1003"));
                    posting.post(
                            "DEMO:OPENING",
                            "OPENING",
                            "教学期初余额",
                            List.of(
                                    new PostingLine("SYS-CLEARING", new BigDecimal("-300000.00")),
                                    new PostingLine("A1001", new BigDecimal("100000.00")),
                                    new PostingLine("A1002", new BigDecimal("100000.00")),
                                    new PostingLine("A1003", new BigDecimal("100000.00"))),
                            locked);
                    product("稳健学堂 90 天", "低风险教学产品，用于演示额度与风险适配。", "LOW", "0.025", "100.00", 90);
                    product("成长学堂 180 天", "中风险教学产品，演示事务内的申购扣款。", "MEDIUM", "0.038", "1000.00", 180);
                    product(
                            "进阶学堂 365 天",
                            "高风险教学产品，可用低风险客户测试拒绝路径。",
                            "HIGH",
                            "0.052",
                            "5000.00",
                            365);
                });
    }

    private void staff(String username, String name, String role) {
        jdbc.sql(
                        "INSERT INTO staff_user(username,password_hash,display_name,role) VALUES (:username,:hash,:name,:role)")
                .param("username", username)
                .param("hash", encoder.encode("Learn@123456"))
                .param("name", name)
                .param("role", role)
                .update();
    }

    private void account(String id, Long customer, String name, boolean internal) {
        accounts.create(
                new Account(
                        id,
                        "DEMO-" + id,
                        customer,
                        name,
                        new BigDecimal("0.00"),
                        AccountStatus.ACTIVE,
                        internal,
                        OffsetDateTime.now(clock)));
    }

    private void product(
            String name, String description, String risk, String rate, String minimum, int days) {
        jdbc.sql(
                        "INSERT INTO financial_product(name,description,risk_level,annual_rate,minimum_amount,remaining_quota,duration_days) VALUES (:name,:description,:risk,:rate,:minimum,500000,:days)")
                .param("name", name)
                .param("description", description)
                .param("risk", risk)
                .param("rate", new BigDecimal(rate))
                .param("minimum", new BigDecimal(minimum))
                .param("days", days)
                .update();
    }
}
