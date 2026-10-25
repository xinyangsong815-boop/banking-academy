package cn.academy.bank.account.domain;

import cn.academy.bank.shared.domain.BusinessException;
import cn.academy.bank.shared.domain.Money;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Account(
        String id,
        String accountNo,
        Long customerId,
        String name,
        BigDecimal balance,
        AccountStatus status,
        boolean internalAccount,
        OffsetDateTime createdAt) {
    public BigDecimal balanceAfter(BigDecimal change) {
        BusinessException.require(
                status == AccountStatus.ACTIVE, "ACCOUNT_UNAVAILABLE", "账户未处于正常状态");
        BigDecimal result = new Money(balance.add(change)).value();
        BusinessException.require(
                internalAccount || result.signum() >= 0, "INSUFFICIENT_FUNDS", "账户余额不足");
        return result;
    }

    public void requireCustomerAccount() {
        BusinessException.require(!internalAccount, "FORBIDDEN", "此操作不允许使用内部账户");
    }
}
