package cn.academy.bank.config;

import cn.academy.bank.transfer.domain.AccountAvailabilityRule;
import cn.academy.bank.transfer.domain.FeePolicy;
import cn.academy.bank.transfer.domain.InstantFeePolicy;
import cn.academy.bank.transfer.domain.StandardFeePolicy;
import cn.academy.bank.transfer.domain.TransferLimitRule;
import cn.academy.bank.transfer.domain.TransferRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class BusinessPolicyConfiguration {
    @Bean
    FeePolicy standardFee() {
        return new StandardFeePolicy();
    }

    @Bean
    FeePolicy instantFee() {
        return new InstantFeePolicy();
    }

    @Bean
    @Order(10)
    TransferRule availabilityRule() {
        return new AccountAvailabilityRule();
    }

    @Bean
    @Order(20)
    TransferRule transferLimitRule() {
        return new TransferLimitRule();
    }
}
