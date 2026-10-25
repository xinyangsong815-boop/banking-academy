package cn.academy.bank.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 值对象：禁止隐式四舍五入，禁止用 double 构造金额。 */
public record Money(BigDecimal value) {
    public Money {
        if (value == null) {
            throw new IllegalArgumentException("金额不能为空");
        }
        try {
            value = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessException("INVALID_MONEY", "金额最多两位小数");
        }
        BusinessException.require(
                value.abs().compareTo(new BigDecimal("999999999999.99")) <= 0,
                "INVALID_MONEY",
                "金额超过支持范围");
    }

    public static Money positive(BigDecimal value) {
        Money money = new Money(value);
        BusinessException.require(money.value.signum() > 0, "INVALID_MONEY", "金额必须大于零");
        return money;
    }
}
