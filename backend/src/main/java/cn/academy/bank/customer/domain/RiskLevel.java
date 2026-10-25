package cn.academy.bank.customer.domain;

/** 本教学模型把 riskLevel 解释为客户风险承受能力，不是信用风险。 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public boolean accepts(RiskLevel productRisk) {
        return ordinal() >= productRisk.ordinal();
    }
}
