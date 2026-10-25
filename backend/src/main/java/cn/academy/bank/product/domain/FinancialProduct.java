package cn.academy.bank.product.domain;

import cn.academy.bank.customer.domain.RiskLevel;
import java.math.BigDecimal;

public record FinancialProduct(
        long id,
        String name,
        String description,
        RiskLevel riskLevel,
        BigDecimal annualRate,
        BigDecimal minimumAmount,
        BigDecimal remainingQuota,
        int durationDays,
        boolean active) {}
