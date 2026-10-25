package cn.academy.bank.loan.domain;

import static cn.academy.bank.shared.domain.BusinessException.require;

import cn.academy.bank.shared.domain.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 模板方法：期数循环、日期和尾差由父类控制，各策略仅决定本期本金。 */
public abstract class RepaymentCalculator {
    public abstract RepaymentMethod method();

    public final List<RepaymentPeriod> calculate(
            BigDecimal principal, int months, BigDecimal annualRate, LocalDate start) {
        principal = Money.positive(principal).value();
        require(months >= 1 && months <= 60, "INVALID_TERM", "期限须为 1~60 个月");
        require(
                annualRate.signum() >= 0 && annualRate.compareTo(BigDecimal.ONE) < 0,
                "INVALID_RATE",
                "年利率应在 0~1 之间");
        var result = new ArrayList<RepaymentPeriod>();
        BigDecimal remaining = principal;
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 16, RoundingMode.HALF_UP);
        for (int period = 1; period <= months; period++) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal part =
                    period == months
                            ? remaining
                            : principalFor(principal, remaining, months, monthlyRate, interest);
            part = part.max(BigDecimal.ZERO).min(remaining).setScale(2, RoundingMode.HALF_UP);
            result.add(new RepaymentPeriod(period, start.plusMonths(period), part, interest));
            remaining = remaining.subtract(part);
        }
        return List.copyOf(result);
    }

    protected abstract BigDecimal principalFor(
            BigDecimal original,
            BigDecimal remaining,
            int months,
            BigDecimal monthlyRate,
            BigDecimal interest);
}
