package cn.academy.bank.transfer.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InstantFeePolicy implements FeePolicy {
    @Override
    public TransferChannel channel() {
        return TransferChannel.INSTANT;
    }

    @Override
    public BigDecimal calculate(BigDecimal amount) {
        return amount.multiply(new BigDecimal("0.001"))
                .max(BigDecimal.ONE)
                .min(new BigDecimal("20"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
