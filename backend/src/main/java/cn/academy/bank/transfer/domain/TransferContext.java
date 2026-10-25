package cn.academy.bank.transfer.domain;

import cn.academy.bank.account.domain.Account;
import java.math.BigDecimal;

public record TransferContext(
        Account source,
        Account target,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal dailyTransferred) {}
