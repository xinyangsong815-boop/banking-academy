package cn.academy.bank.product.application;

import static cn.academy.bank.shared.domain.BusinessException.require;

import cn.academy.bank.audit.application.Audited;
import cn.academy.bank.customer.application.CustomerService;
import cn.academy.bank.identity.application.CurrentStaff;
import cn.academy.bank.ledger.application.PostingService;
import cn.academy.bank.ledger.domain.PostingLine;
import cn.academy.bank.notification.domain.BusinessCompleted;
import cn.academy.bank.product.domain.FinancialProduct;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.domain.BusinessException;
import cn.academy.bank.shared.domain.Money;
import cn.academy.bank.shared.infrastructure.IdempotencyService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final JdbcClient jdbc;
    private final PostingService posting;
    private final CustomerService customers;
    private final CurrentStaff current;
    private final IdempotencyService idempotency;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ProductService(
            JdbcClient jdbc,
            PostingService posting,
            CustomerService customers,
            CurrentStaff current,
            IdempotencyService idempotency,
            ApplicationEventPublisher events,
            Clock clock) {
        this.jdbc = jdbc;
        this.posting = posting;
        this.customers = customers;
        this.current = current;
        this.idempotency = idempotency;
        this.events = events;
        this.clock = clock;
    }

    public record Subscribe(long productId, String accountId, BigDecimal amount) {}

    public record Subscription(
            String id,
            long productId,
            String accountId,
            BigDecimal amount,
            OffsetDateTime createdAt) {}

    public List<FinancialProduct> products() {
        return jdbc.sql("SELECT * FROM financial_product ORDER BY id")
                .query(FinancialProduct.class)
                .list();
    }

    @Audited("PRODUCT_SUBSCRIBE")
    @PreAuthorize("hasAuthority('product:write')")
    public Subscription subscribe(String key, Subscribe command) {
        var normalized =
                new Subscribe(
                        command.productId(),
                        command.accountId(),
                        Money.positive(command.amount()).value());
        return idempotency.execute(
                "subscribe:" + current.get().id(),
                key,
                normalized,
                Subscription.class,
                () -> execute(normalized));
    }

    private Subscription execute(Subscribe command) {
        var product =
                jdbc.sql("SELECT * FROM financial_product WHERE id=:id FOR UPDATE")
                        .param("id", command.productId())
                        .query(FinancialProduct.class)
                        .optional()
                        .orElseThrow(() -> BusinessException.notFound("产品"));
        var locked = posting.lockAccounts(List.of(command.accountId(), "SYS-PRODUCT"));
        var account = locked.get(command.accountId());
        account.requireCustomerAccount();
        require(product.active(), "PRODUCT_INACTIVE", "产品已下架");
        require(
                customers.get(account.customerId()).riskLevel().accepts(product.riskLevel()),
                "RISK_MISMATCH",
                "客户风险承受能力不满足产品要求");
        require(
                command.amount().compareTo(product.minimumAmount()) >= 0,
                "BELOW_MINIMUM",
                "申购金额低于起购金额");
        require(
                command.amount().compareTo(product.remainingQuota()) <= 0,
                "QUOTA_EXCEEDED",
                "产品剩余额度不足");
        String id = UUID.randomUUID().toString();
        posting.post(
                "SUBSCRIBE:" + id,
                "SUBSCRIBE",
                "产品申购",
                List.of(
                        new PostingLine(command.accountId(), command.amount().negate()),
                        new PostingLine("SYS-PRODUCT", command.amount())),
                locked);
        jdbc.sql(
                        "UPDATE financial_product SET remaining_quota=remaining_quota-:amount WHERE id=:id")
                .param("amount", command.amount())
                .param("id", product.id())
                .update();
        var result =
                new Subscription(
                        id,
                        product.id(),
                        account.id(),
                        command.amount(),
                        OffsetDateTime.now(clock));
        jdbc.sql(
                        "INSERT INTO product_subscription(id,product_id,account_id,amount,created_at) VALUES (:id,:product,:account,:amount,:now)")
                .param("id", id)
                .param("product", product.id())
                .param("account", account.id())
                .param("amount", command.amount())
                .param("now", result.createdAt())
                .update();
        events.publishEvent(
                new BusinessCompleted(
                        current.get().id(),
                        "产品申购成功",
                        product.name() + "，金额 " + command.amount() + " 元"));
        return result;
    }

    public PageResult<Subscription> subscriptions(int page, int size) {
        int offset = PageResult.offset(page, size);
        var items =
                jdbc.sql(
                                "SELECT * FROM product_subscription ORDER BY created_at DESC,id LIMIT :size OFFSET :offset")
                        .param("size", size)
                        .param("offset", offset)
                        .query(Subscription.class)
                        .list();
        return new PageResult<>(
                items,
                jdbc.sql("SELECT COUNT(*) FROM product_subscription").query(Long.class).single(),
                page,
                size);
    }
}
