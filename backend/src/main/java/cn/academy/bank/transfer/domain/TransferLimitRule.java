package cn.academy.bank.transfer.domain;

import static cn.academy.bank.shared.domain.BusinessException.require;

import java.math.BigDecimal;

public class TransferLimitRule implements TransferRule {
    @Override
    public void check(TransferContext context) {
        require(
                context.amount().compareTo(new BigDecimal("20000")) <= 0,
                "SINGLE_LIMIT",
                "单笔转账上限为 20,000 元");
        require(
                context.dailyTransferred().add(context.amount()).compareTo(new BigDecimal("50000"))
                        <= 0,
                "DAILY_LIMIT",
                "账户每日累计转账上限为 50,000 元");
    }
}
