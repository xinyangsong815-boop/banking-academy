package cn.academy.bank.loan.web;

import cn.academy.bank.loan.application.LoanService;
import cn.academy.bank.loan.domain.LoanApplication;
import cn.academy.bank.loan.domain.RepaymentMethod;
import cn.academy.bank.loan.domain.RepaymentPeriod;
import cn.academy.bank.shared.api.ApiResponse;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.validation.MoneyAmount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    public record ApplyRequest(
            @Positive long customerId,
            @NotBlank @Size(max = 36) String accountId,
            @MoneyAmount BigDecimal principal,
            @Min(1) @Max(60) int months,
            @NotNull RepaymentMethod repaymentMethod,
            @NotBlank @Size(max = 160) String purpose) {}

    public record ReviewRequest(
            @NotNull Boolean approved, @NotBlank @Size(max = 200) String note) {}

    @PostMapping
    ApiResponse<LoanApplication> apply(@Valid @RequestBody ApplyRequest request) {
        return ApiResponse.ok(
                service.apply(
                        new LoanService.ApplyCommand(
                                request.customerId(),
                                request.accountId(),
                                request.principal(),
                                request.months(),
                                request.repaymentMethod(),
                                request.purpose())));
    }

    @GetMapping
    ApiResponse<PageResult<LoanApplication>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.list(page, size));
    }

    @PostMapping("/{id}/review")
    ApiResponse<LoanApplication> review(
            @PathVariable String id, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(service.review(id, request.approved(), request.note()));
    }

    @PostMapping("/{id}/disburse")
    ApiResponse<LoanApplication> disburse(@PathVariable String id) {
        return ApiResponse.ok(service.disburse(id));
    }

    @GetMapping("/{id}/plan")
    ApiResponse<List<RepaymentPeriod>> plan(@PathVariable String id) {
        return ApiResponse.ok(service.plan(id));
    }
}
