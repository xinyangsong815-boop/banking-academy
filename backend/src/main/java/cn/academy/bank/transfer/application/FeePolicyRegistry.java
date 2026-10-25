package cn.academy.bank.transfer.application;

import cn.academy.bank.transfer.domain.FeePolicy;
import cn.academy.bank.transfer.domain.TransferChannel;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 注册表替代散落的 switch；启动时检查策略重复和缺失。 */
@Component
public class FeePolicyRegistry {
    private final Map<TransferChannel, FeePolicy> policies = new EnumMap<>(TransferChannel.class);

    public FeePolicyRegistry(List<FeePolicy> implementations) {
        for (FeePolicy policy : implementations) {
            if (policies.putIfAbsent(policy.channel(), policy) != null) {
                throw new IllegalStateException("重复费用策略: " + policy.channel());
            }
        }
        if (policies.size() != TransferChannel.values().length) {
            throw new IllegalStateException("存在未配置的转账渠道");
        }
    }

    public BigDecimal fee(TransferChannel channel, BigDecimal amount) {
        return policies.get(channel).calculate(amount);
    }
}
