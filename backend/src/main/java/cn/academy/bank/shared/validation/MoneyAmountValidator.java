package cn.academy.bank.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class MoneyAmountValidator implements ConstraintValidator<MoneyAmount, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        return value != null
                && value.signum() > 0
                && value.scale() <= 2
                && value.compareTo(new BigDecimal("999999999999.99")) <= 0;
    }
}
