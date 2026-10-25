package cn.academy.bank.loan.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepaymentPeriod(
        int periodNo, LocalDate dueDate, BigDecimal principal, BigDecimal interest) {}
