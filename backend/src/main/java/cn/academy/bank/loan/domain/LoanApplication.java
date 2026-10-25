package cn.academy.bank.loan.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LoanApplication(
        String id,
        long customerId,
        String accountId,
        BigDecimal principal,
        int months,
        BigDecimal annualRate,
        RepaymentMethod repaymentMethod,
        String purpose,
        LoanState state,
        long applicantId,
        Long reviewerId,
        String reviewNote,
        OffsetDateTime createdAt) {}
