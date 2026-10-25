package cn.academy.bank.transfer.domain;

/** 规则流水线：每条规则只判断一个维度，顺序由装配层决定。 */
@FunctionalInterface
public interface TransferRule {
    void check(TransferContext context);
}
