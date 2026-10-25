package cn.academy.bank.loan.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EqualPrincipalCalculator extends RepaymentCalculator {
    @Override
    public RepaymentMethod method() {
        return RepaymentMethod.EQUAL_PRINCIPAL;
    }

    @Override
    protected BigDecimal principalFor(
            BigDecimal original,
            BigDecimal remaining,
            int months,
            BigDecimal monthlyRate,
            BigDecimal interest) {
        return original.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }
}
