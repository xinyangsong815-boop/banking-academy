package cn.academy.bank.loan.domain;

import static cn.academy.bank.shared.domain.BusinessException.require;

public enum LoanState {
    SUBMITTED,
    APPROVED,
    REJECTED,
    DISBURSED;

    public void requireReviewable() {
        require(this == SUBMITTED, "INVALID_STATE", "只有待审批贷款可以审核");
    }

    public void requireDisbursable() {
        require(this == APPROVED, "INVALID_STATE", "只有已批准贷款可以放款");
    }
}
