package cn.academy.bank.account.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> find(String id);

    Account lock(String id);

    List<Account> search(Long customerId, int offset, int size);

    long count(Long customerId);

    void create(Account account);

    void updateBalance(String id, BigDecimal balance);

    void updateStatus(String id, AccountStatus status);
}
