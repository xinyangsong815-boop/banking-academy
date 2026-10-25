package cn.academy.bank.transfer.domain;

import java.math.BigDecimal;

public class StandardFeePolicy implements FeePolicy {
    @Override
    public TransferChannel channel() {
        return TransferChannel.STANDARD;
    }

    @Override
    public BigDecimal calculate(BigDecimal amount) {
        return new BigDecimal("0.00");
    }
}
