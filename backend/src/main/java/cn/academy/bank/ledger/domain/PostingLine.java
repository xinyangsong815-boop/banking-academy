package cn.academy.bank.ledger.domain;

import java.math.BigDecimal;

/** 正数增加账户余额，负数减少；一组分录的金额之和必须为零。 */
public record PostingLine(String accountId, BigDecimal change) {}
