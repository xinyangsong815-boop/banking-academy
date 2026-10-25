package cn.academy.bank.account.application;

import cn.academy.bank.account.domain.Account;
import cn.academy.bank.account.domain.AccountRepository;
import cn.academy.bank.account.domain.AccountStatus;
import cn.academy.bank.audit.application.Audited;
import cn.academy.bank.customer.application.CustomerService;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.domain.BusinessException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final CustomerService customers;
    private final Clock clock;

    public AccountService(AccountRepository accounts, CustomerService customers, Clock clock) {
        this.accounts = accounts;
        this.customers = customers;
        this.clock = clock;
    }

    public Account get(String id) {
        Account account = accounts.find(id).orElseThrow(() -> BusinessException.notFound("账户"));
        account.requireCustomerAccount();
        return account;
    }

    public PageResult<Account> search(Long customerId, int page, int size) {
        int offset = PageResult.offset(page, size);
        return new PageResult<>(
                accounts.search(customerId, offset, size), accounts.count(customerId), page, size);
    }

    @Transactional
    @Audited("ACCOUNT_OPEN")
    @PreAuthorize("hasAuthority('account:write')")
    public Account open(long customerId, String name) {
        customers.get(customerId);
        String id = UUID.randomUUID().toString();
        String number =
                "AC"
                        + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 20)
                                .toUpperCase(java.util.Locale.ROOT);
        Account account =
                new Account(
                        id,
                        number,
                        customerId,
                        name.strip(),
                        new BigDecimal("0.00"),
                        AccountStatus.ACTIVE,
                        false,
                        OffsetDateTime.now(clock));
        accounts.create(account);
        return account;
    }

    @Transactional
    @Audited("ACCOUNT_STATUS_CHANGE")
    @PreAuthorize("hasAuthority('account:write')")
    public Account changeStatus(String id, AccountStatus next) {
        Account account = accounts.lock(id);
        account.requireCustomerAccount();
        account.status().requireTransitionTo(next);
        BusinessException.require(
                next != AccountStatus.CLOSED || account.balance().signum() == 0,
                "NONZERO_BALANCE",
                "余额为零时才允许销户");
        accounts.updateStatus(id, next);
        return accounts.find(id).orElseThrow();
    }
}
