package cn.academy.bank.account.web;

import cn.academy.bank.account.application.AccountService;
import cn.academy.bank.account.domain.Account;
import cn.academy.bank.account.domain.AccountStatus;
import cn.academy.bank.ledger.application.LedgerQueryService;
import cn.academy.bank.ledger.domain.LedgerEntry;
import cn.academy.bank.shared.api.ApiResponse;
import cn.academy.bank.shared.api.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;
    private final LedgerQueryService ledger;

    public AccountController(AccountService accounts, LedgerQueryService ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    public record OpenAccount(@Positive long customerId, @NotBlank @Size(max = 60) String name) {}

    public record StatusChange(@NotNull AccountStatus status) {}

    @GetMapping
    ApiResponse<PageResult<Account>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(accounts.search(customerId, page, size));
    }

    @PostMapping
    ApiResponse<Account> open(@Valid @RequestBody OpenAccount request) {
        return ApiResponse.ok(accounts.open(request.customerId(), request.name()));
    }

    @PutMapping("/{id}/status")
    ApiResponse<Account> status(@PathVariable String id, @Valid @RequestBody StatusChange request) {
        return ApiResponse.ok(accounts.changeStatus(id, request.status()));
    }

    @GetMapping("/{id}/entries")
    ApiResponse<PageResult<LedgerEntry>> entries(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        accounts.get(id);
        return ApiResponse.ok(ledger.entries(id, page, size));
    }
}
