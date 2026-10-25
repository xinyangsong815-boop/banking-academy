package cn.academy.bank.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.academy.bank.account.domain.AccountStatus;
import cn.academy.bank.customer.domain.RiskLevel;
import cn.academy.bank.loan.domain.EqualInstallmentCalculator;
import cn.academy.bank.loan.domain.EqualPrincipalCalculator;
import cn.academy.bank.loan.domain.RepaymentCalculator;
import cn.academy.bank.loan.domain.RepaymentPeriod;
import cn.academy.bank.shared.domain.BusinessException;
import cn.academy.bank.shared.domain.Money;
import cn.academy.bank.transfer.domain.InstantFeePolicy;
import cn.academy.bank.transfer.domain.StandardFeePolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyAndPolicyTest {
    @Test
    void rejectsFractionalCentAndNonpositiveAmount() {
        assertThatThrownBy(() -> Money.positive(new BigDecimal("0")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Money.positive(new BigDecimal("1.001")))
                .isInstanceOf(BusinessException.class);
        assertThat(new Money(new BigDecimal("1.000")).value()).isEqualByComparingTo("1.00");
    }

    @ParameterizedTest
    @CsvSource({"0.01,1.00", "1000,1.00", "1234.56,1.23", "20000,20.00", "99999,20.00"})
    void feeHasFloorCapAndExplicitRounding(String amount, String expected) {
        assertThat(new InstantFeePolicy().calculate(new BigDecimal(amount)))
                .isEqualByComparingTo(expected);
        assertThat(new StandardFeePolicy().calculate(new BigDecimal(amount)))
                .isEqualByComparingTo("0");
    }

    @Test
    void closedAccountsCannotBeReopened() {
        assertThatThrownBy(() -> AccountStatus.CLOSED.requireTransitionTo(AccountStatus.ACTIVE))
                .isInstanceOf(BusinessException.class);
        assertThatCode(() -> AccountStatus.FROZEN.requireTransitionTo(AccountStatus.ACTIVE))
                .doesNotThrowAnyException();
    }

    @Test
    void productRiskMustNotExceedTolerance() {
        assertThat(RiskLevel.LOW.accepts(RiskLevel.HIGH)).isFalse();
        assertThat(RiskLevel.HIGH.accepts(RiskLevel.LOW)).isTrue();
    }

    @Test
    void bothRepaymentAlgorithmsConservePrincipalIncludingLastCent() {
        for (RepaymentCalculator calculator :
                List.of(new EqualPrincipalCalculator(), new EqualInstallmentCalculator())) {
            var plan =
                    calculator.calculate(
                            new BigDecimal("10000.01"),
                            36,
                            new BigDecimal("0.048"),
                            LocalDate.of(2026, 1, 31));
            assertThat(plan).hasSize(36);
            assertThat(
                            plan.stream()
                                    .map(RepaymentPeriod::principal)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .isEqualByComparingTo("10000.01");
            assertThat(plan.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(plan)
                    .allSatisfy(
                            period -> {
                                assertThat(period.principal()).isNotNegative();
                                assertThat(period.interest()).isNotNegative();
                            });
        }
    }

    @Test
    void equalInstallmentsAlsoSupportZeroRateAndOnePeriod() {
        var plan =
                new EqualInstallmentCalculator()
                        .calculate(new BigDecimal("100"), 3, BigDecimal.ZERO, LocalDate.now());
        assertThat(plan.get(2).principal()).isEqualByComparingTo("33.34");
        assertThat(plan).allSatisfy(period -> assertThat(period.interest()).isZero());
        var one =
                new EqualPrincipalCalculator()
                        .calculate(
                                new BigDecimal("100"), 1, new BigDecimal("0.12"), LocalDate.now());
        assertThat(one.get(0).principal()).isEqualByComparingTo("100");
        assertThat(one.get(0).interest()).isEqualByComparingTo("1");
    }
}
