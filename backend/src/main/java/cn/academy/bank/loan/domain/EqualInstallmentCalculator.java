package cn.academy.bank.loan.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EqualInstallmentCalculator extends RepaymentCalculator {
    @Override
    public RepaymentMethod method() {
        return RepaymentMethod.EQUAL_INSTALLMENT;
    }

    @Override
    protected BigDecimal principalFor(
            BigDecimal original,
            BigDecimal remaining,
            int months,
            BigDecimal monthlyRate,
            BigDecimal interest) {
        if (monthlyRate.signum() == 0) {
            return original.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months);
        BigDecimal installment =
                original.multiply(monthlyRate)
                        .multiply(factor)
                        .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        return installment.subtract(interest);
    }
}
