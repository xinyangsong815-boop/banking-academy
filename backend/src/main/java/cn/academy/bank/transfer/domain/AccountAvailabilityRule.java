package cn.academy.bank.transfer.domain;

import static cn.academy.bank.shared.domain.BusinessException.require;

import cn.academy.bank.account.domain.AccountStatus;

public class AccountAvailabilityRule implements TransferRule {
    @Override
    public void check(TransferContext context) {
        context.source().requireCustomerAccount();
        context.target().requireCustomerAccount();
        require(!context.source().id().equals(context.target().id()), "SAME_ACCOUNT", "不能向同一账户转账");
        require(
                context.source().status() == AccountStatus.ACTIVE
                        && context.target().status() == AccountStatus.ACTIVE,
                "ACCOUNT_UNAVAILABLE",
                "转出与转入账户均需处于正常状态");
        require(
                context.source().balance().compareTo(context.amount().add(context.fee())) >= 0,
                "INSUFFICIENT_FUNDS",
                "余额不足以支付转账金额与手续费");
    }
}
