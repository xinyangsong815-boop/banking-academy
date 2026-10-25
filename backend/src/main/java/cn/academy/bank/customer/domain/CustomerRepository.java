package cn.academy.bank.customer.domain;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> find(long id);

    List<Customer> search(String keyword, int offset, int size);

    long count(String keyword);

    long save(String name, String phone, String email, RiskLevel riskLevel);

    void update(long id, String name, String phone, String email, RiskLevel riskLevel);
}
