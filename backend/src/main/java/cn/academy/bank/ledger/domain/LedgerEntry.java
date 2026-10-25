package cn.academy.bank.ledger.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LedgerEntry(
        long id,
        String bookingId,
        String accountId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String kind,
        String description,
        OffsetDateTime createdAt) {}
