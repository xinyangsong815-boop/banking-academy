package cn.academy.bank.transfer.domain;

import java.math.BigDecimal;

/** 策略模式：费用算法与转账事务编排分离。 */
public interface FeePolicy {
    TransferChannel channel();

    BigDecimal calculate(BigDecimal amount);
}
