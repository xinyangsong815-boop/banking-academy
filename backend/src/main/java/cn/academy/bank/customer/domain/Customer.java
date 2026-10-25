package cn.academy.bank.customer.domain;

import java.time.OffsetDateTime;

public record Customer(
        long id,
        String name,
        String phone,
        String email,
        RiskLevel riskLevel,
        OffsetDateTime createdAt) {}
