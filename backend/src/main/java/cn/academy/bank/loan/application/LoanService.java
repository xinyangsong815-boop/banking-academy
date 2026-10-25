package cn.academy.bank.loan.application;

import static cn.academy.bank.shared.domain.BusinessException.require;

import cn.academy.bank.account.application.AccountService;
import cn.academy.bank.audit.application.Audited;
import cn.academy.bank.identity.application.CurrentStaff;
import cn.academy.bank.ledger.application.PostingService;
import cn.academy.bank.ledger.domain.PostingLine;
import cn.academy.bank.loan.domain.EqualInstallmentCalculator;
import cn.academy.bank.loan.domain.EqualPrincipalCalculator;
import cn.academy.bank.loan.domain.LoanApplication;
import cn.academy.bank.loan.domain.LoanState;
import cn.academy.bank.loan.domain.RepaymentCalculator;
import cn.academy.bank.loan.domain.RepaymentMethod;
import cn.academy.bank.loan.domain.RepaymentPeriod;
import cn.academy.bank.notification.domain.BusinessCompleted;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.domain.BusinessException;
import cn.academy.bank.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {
    private final JdbcClient jdbc;
    private final AccountService accounts;
    private final PostingService posting;
    private final CurrentStaff current;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Map<RepaymentMethod, RepaymentCalculator> calculators =
            Map.of(
                    RepaymentMethod.EQUAL_PRINCIPAL, new EqualPrincipalCalculator(),
                    RepaymentMethod.EQUAL_INSTALLMENT, new EqualInstallmentCalculator());

    public LoanService(
            JdbcClient jdbc,
            AccountService accounts,
            PostingService posting,
            CurrentStaff current,
            ApplicationEventPublisher events,
            Clock clock) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.posting = posting;
        this.current = current;
        this.events = events;
        this.clock = clock;
    }

    public record ApplyCommand(
            long customerId,
            String accountId,
            BigDecimal principal,
            int months,
            RepaymentMethod repaymentMethod,
            String purpose) {}

    @Transactional
    @Audited("LOAN_APPLY")
    @PreAuthorize("hasAuthority('loan:apply')")
    public LoanApplication apply(ApplyCommand command) {
        var account = accounts.get(command.accountId());
        require(account.customerId() == command.customerId(), "CUSTOMER_MISMATCH", "贷款客户与收款账户不匹配");
        var amount = Money.positive(command.principal()).value();
        require(amount.compareTo(new BigDecimal("200000")) <= 0, "LOAN_LIMIT", "教学贷款上限为 200,000 元");
        String id = UUID.randomUUID().toString();
        jdbc.sql(
                        """
                INSERT INTO loan_application(id,customer_id,account_id,principal,months,annual_rate,repayment_method,
                    purpose,state,applicant_id,created_at)
                VALUES (:id,:customer,:account,:principal,:months,0.048,:method,:purpose,'SUBMITTED',:actor,:now)
                """)
                .param("id", id)
                .param("customer", command.customerId())
                .param("account", command.accountId())
                .param("principal", amount)
                .param("months", command.months())
                .param("method", command.repaymentMethod().name())
                .param("purpose", command.purpose())
                .param("actor", current.get().id())
                .param("now", OffsetDateTime.now(clock))
                .update();
        return get(id);
    }

    @Transactional
    @Audited("LOAN_REVIEW")
    @PreAuthorize("hasAuthority('loan:review')")
    public LoanApplication review(String id, boolean approved, String note) {
        LoanApplication loan = lock(id);
        loan.state().requireReviewable();
        require(loan.applicantId() != current.get().id(), "MAKER_CHECKER", "申请人与审核人必须不同");
        LoanState next = approved ? LoanState.APPROVED : LoanState.REJECTED;
        jdbc.sql(
                        "UPDATE loan_application SET state=:state,reviewer_id=:reviewer,review_note=:note WHERE id=:id")
                .param("state", next.name())
                .param("reviewer", current.get().id())
                .param("note", note)
                .param("id", id)
                .update();
        events.publishEvent(
                new BusinessCompleted(loan.applicantId(), "贷款审核完成", "业务号 " + id + "，结果 " + next));
        return get(id);
    }

    @Transactional
    @Audited("LOAN_DISBURSE")
    @PreAuthorize("hasAuthority('loan:disburse')")
    public LoanApplication disburse(String id) {
        LoanApplication loan = lock(id);
        loan.state().requireDisbursable();
        var locked = posting.lockAccounts(List.of("SYS-CLEARING", loan.accountId()));
        posting.post(
                "LOAN:" + id,
                "LOAN",
                "贷款放款",
                List.of(
                        new PostingLine("SYS-CLEARING", loan.principal().negate()),
                        new PostingLine(loan.accountId(), loan.principal())),
                locked);
        // 计划从实际放款日计算，避免审批后等待放款导致首期日期提前。
        LocalDate start = LocalDate.now(clock.withZone(ZoneId.of("Asia/Shanghai")));
        for (RepaymentPeriod period :
                calculators
                        .get(loan.repaymentMethod())
                        .calculate(loan.principal(), loan.months(), loan.annualRate(), start)) {
            jdbc.sql(
                            "INSERT INTO repayment_plan(loan_id,period_no,due_date,principal,interest) VALUES (:loan,:period,:due,:principal,:interest)")
                    .param("loan", id)
                    .param("period", period.periodNo())
                    .param("due", period.dueDate())
                    .param("principal", period.principal())
                    .param("interest", period.interest())
                    .update();
        }
        jdbc.sql("UPDATE loan_application SET state='DISBURSED' WHERE id=:id")
                .param("id", id)
                .update();
        events.publishEvent(
                new BusinessCompleted(
                        loan.applicantId(),
                        "贷款已放款",
                        "业务号 " + id + "，本金 " + loan.principal() + " 元"));
        return get(id);
    }

    public LoanApplication get(String id) {
        return jdbc.sql("SELECT * FROM loan_application WHERE id=:id")
                .param("id", id)
                .query(LoanApplication.class)
                .optional()
                .orElseThrow(() -> BusinessException.notFound("贷款"));
    }

    private LoanApplication lock(String id) {
        return jdbc.sql("SELECT * FROM loan_application WHERE id=:id FOR UPDATE")
                .param("id", id)
                .query(LoanApplication.class)
                .optional()
                .orElseThrow(() -> BusinessException.notFound("贷款"));
    }

    public PageResult<LoanApplication> list(int page, int size) {
        int offset = PageResult.offset(page, size);
        var items =
                jdbc.sql(
                                "SELECT * FROM loan_application ORDER BY created_at DESC,id LIMIT :size OFFSET :offset")
                        .param("size", size)
                        .param("offset", offset)
                        .query(LoanApplication.class)
                        .list();
        return new PageResult<>(
                items,
                jdbc.sql("SELECT COUNT(*) FROM loan_application").query(Long.class).single(),
                page,
                size);
    }

    public List<RepaymentPeriod> plan(String id) {
        get(id);
        return jdbc.sql(
                        "SELECT period_no,due_date,principal,interest FROM repayment_plan WHERE loan_id=:id ORDER BY period_no")
                .param("id", id)
                .query(RepaymentPeriod.class)
                .list();
    }
}
