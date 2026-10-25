package cn.academy.bank.customer.application;

import cn.academy.bank.audit.application.Audited;
import cn.academy.bank.customer.domain.Customer;
import cn.academy.bank.customer.domain.CustomerRepository;
import cn.academy.bank.customer.domain.RiskLevel;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.domain.BusinessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    public Customer get(long id) {
        return customers.find(id).orElseThrow(() -> BusinessException.notFound("客户"));
    }

    public PageResult<Customer> search(String keyword, int page, int size) {
        int offset = PageResult.offset(page, size);
        return new PageResult<>(
                customers.search(keyword, offset, size), customers.count(keyword), page, size);
    }

    @Transactional
    @Audited("CUSTOMER_CREATE")
    @PreAuthorize("hasAuthority('customer:write')")
    public Customer create(String name, String phone, String email, RiskLevel riskLevel) {
        long id = customers.save(name.strip(), phone, email, riskLevel);
        return get(id);
    }

    @Transactional
    @Audited("CUSTOMER_UPDATE")
    @PreAuthorize("hasAuthority('customer:write')")
    public Customer update(long id, String name, String phone, String email, RiskLevel riskLevel) {
        get(id);
        customers.update(id, name.strip(), phone, email, riskLevel);
        return get(id);
    }
}
