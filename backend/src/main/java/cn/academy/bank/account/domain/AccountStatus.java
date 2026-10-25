package cn.academy.bank.account.domain;

import cn.academy.bank.shared.domain.BusinessException;

/** 枚举实现的小型状态机；复杂状态行为再拆成独立 State 类。 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED;

    public void requireTransitionTo(AccountStatus next) {
        boolean allowed =
                switch (this) {
                    case ACTIVE -> next == FROZEN || next == CLOSED;
                    case FROZEN -> next == ACTIVE;
                    case CLOSED -> false;
                };
        BusinessException.require(allowed, "INVALID_STATE", "不允许从 " + this + " 变为 " + next);
    }
}
