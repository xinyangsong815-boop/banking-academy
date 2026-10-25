package cn.academy.bank.transfer.web;

import cn.academy.bank.shared.api.ApiResponse;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.validation.MoneyAmount;
import cn.academy.bank.transfer.application.TransferService;
import cn.academy.bank.transfer.domain.TransferChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    public record TransferRequest(
            @NotBlank @Size(max = 36) String sourceId,
            @NotBlank @Size(max = 36) String targetId,
            @MoneyAmount BigDecimal amount,
            @NotNull TransferChannel channel,
            @NotNull @Size(max = 120) String remark) {}

    @PostMapping
    ApiResponse<TransferService.Receipt> transfer(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody TransferRequest request) {
        return ApiResponse.ok(
                service.transfer(
                        key,
                        new TransferService.Command(
                                request.sourceId(),
                                request.targetId(),
                                request.amount(),
                                request.channel(),
                                request.remark())));
    }

    @GetMapping
    ApiResponse<PageResult<TransferService.Receipt>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.list(page, size));
    }
}
