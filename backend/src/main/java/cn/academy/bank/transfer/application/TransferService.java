package cn.academy.bank.transfer.application;

import cn.academy.bank.audit.application.Audited;
import cn.academy.bank.identity.application.CurrentStaff;
import cn.academy.bank.ledger.application.PostingService;
import cn.academy.bank.ledger.domain.PostingLine;
import cn.academy.bank.notification.domain.BusinessCompleted;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.domain.Money;
import cn.academy.bank.shared.infrastructure.IdempotencyService;
import cn.academy.bank.transfer.domain.TransferChannel;
import cn.academy.bank.transfer.domain.TransferContext;
import cn.academy.bank.transfer.domain.TransferRule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private final JdbcClient jdbc;
    private final PostingService posting;
    private final FeePolicyRegistry fees;
    private final List<TransferRule> rules;
    private final IdempotencyService idempotency;
    private final CurrentStaff current;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public TransferService(
            JdbcClient jdbc,
            PostingService posting,
            FeePolicyRegistry fees,
            List<TransferRule> rules,
            IdempotencyService idempotency,
            CurrentStaff current,
            ApplicationEventPublisher events,
            Clock clock) {
        this.jdbc = jdbc;
        this.posting = posting;
        this.fees = fees;
        this.rules = List.copyOf(rules);
        this.idempotency = idempotency;
        this.current = current;
        this.events = events;
        this.clock = clock;
    }

    public record Command(
            String sourceId,
            String targetId,
            BigDecimal amount,
            TransferChannel channel,
            String remark) {}

    public record Receipt(
            String id,
            String sourceId,
            String targetId,
            BigDecimal amount,
            BigDecimal fee,
            TransferChannel channel,
            String remark,
            long actorId,
            OffsetDateTime createdAt) {}

    @Audited("TRANSFER_SUBMIT")
    @PreAuthorize("hasAuthority('transfer:write')")
    public Receipt transfer(String key, Command command) {
        // 先规范化金额，100 和 100.00 产生相同的请求指纹。
        var normalized =
                new Command(
                        command.sourceId(),
                        command.targetId(),
                        Money.positive(command.amount()).value(),
                        command.channel(),
                        command.remark());
        return idempotency.execute(
                "transfer:" + current.get().id(),
                key,
                normalized,
                Receipt.class,
                () -> execute(normalized));
    }

    private Receipt execute(Command command) {
        BigDecimal fee = fees.fee(command.channel(), command.amount());
        List<String> ids = new ArrayList<>(List.of(command.sourceId(), command.targetId()));
        if (fee.signum() > 0) {
            ids.add("SYS-FEE");
        }
        var locked = posting.lockAccounts(ids);
        OffsetDateTime start =
                LocalDate.now(clock.withZone(ZoneId.of("Asia/Shanghai")))
                        .atStartOfDay(ZoneId.of("Asia/Shanghai"))
                        .toOffsetDateTime();
        BigDecimal daily =
                jdbc.sql(
                                "SELECT COALESCE(SUM(amount),0) FROM transfer_order WHERE source_id=:source AND created_at>=:start")
                        .param("source", command.sourceId())
                        .param("start", start)
                        .query(BigDecimal.class)
                        .single();
        var context =
                new TransferContext(
                        locked.get(command.sourceId()),
                        locked.get(command.targetId()),
                        command.amount(),
                        fee,
                        daily);
        rules.forEach(rule -> rule.check(context));
        String id = UUID.randomUUID().toString();
        var lines = new ArrayList<PostingLine>();
        lines.add(new PostingLine(command.sourceId(), command.amount().add(fee).negate()));
        lines.add(new PostingLine(command.targetId(), command.amount()));
        if (fee.signum() > 0) {
            lines.add(new PostingLine("SYS-FEE", fee));
        }
        posting.post("TRANSFER:" + id, "TRANSFER", "账户转账", lines, locked);
        var receipt =
                new Receipt(
                        id,
                        command.sourceId(),
                        command.targetId(),
                        command.amount(),
                        fee,
                        command.channel(),
                        command.remark(),
                        current.get().id(),
                        OffsetDateTime.now(clock));
        jdbc.sql(
                        "INSERT INTO transfer_order(id,source_id,target_id,amount,fee,channel,remark,actor_id,created_at) VALUES (:id,:source,:target,:amount,:fee,:channel,:remark,:actor,:now)")
                .param("id", id)
                .param("source", receipt.sourceId())
                .param("target", receipt.targetId())
                .param("amount", receipt.amount())
                .param("fee", fee)
                .param("channel", receipt.channel().name())
                .param("remark", receipt.remark())
                .param("actor", receipt.actorId())
                .param("now", receipt.createdAt())
                .update();
        events.publishEvent(
                new BusinessCompleted(
                        receipt.actorId(), "转账完成", "业务号 " + id + "，金额 " + receipt.amount() + " 元"));
        return receipt;
    }

    public PageResult<Receipt> list(int page, int size) {
        int offset = PageResult.offset(page, size);
        var items =
                jdbc.sql(
                                "SELECT * FROM transfer_order ORDER BY created_at DESC,id LIMIT :size OFFSET :offset")
                        .param("size", size)
                        .param("offset", offset)
                        .query(Receipt.class)
                        .list();
        long count = jdbc.sql("SELECT COUNT(*) FROM transfer_order").query(Long.class).single();
        return new PageResult<>(items, count, page, size);
    }
}
