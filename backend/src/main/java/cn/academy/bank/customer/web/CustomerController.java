package cn.academy.bank.customer.web;

import cn.academy.bank.customer.application.CustomerService;
import cn.academy.bank.customer.domain.Customer;
import cn.academy.bank.customer.domain.RiskLevel;
import cn.academy.bank.shared.api.ApiResponse;
import cn.academy.bank.shared.api.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    public record CustomerRequest(
            @NotBlank @Size(max = 60) String name,
            @NotBlank @Pattern(regexp = "1[3-9][0-9]{9}") String phone,
            @NotBlank @Email @Size(max = 120) String email,
            @NotNull RiskLevel riskLevel) {}

    @GetMapping
    ApiResponse<PageResult<Customer>> list(
            @RequestParam(defaultValue = "") @Size(max = 60) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.search(keyword, page, size));
    }

    @GetMapping("/{id}")
    ApiResponse<Customer> get(@PathVariable long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    ApiResponse<Customer> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(
                service.create(
                        request.name(), request.phone(), request.email(), request.riskLevel()));
    }

    @PutMapping("/{id}")
    ApiResponse<Customer> update(
            @PathVariable long id, @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(
                service.update(
                        id, request.name(), request.phone(), request.email(), request.riskLevel()));
    }
}
