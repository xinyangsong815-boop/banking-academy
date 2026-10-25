package cn.academy.bank.product.web;

import cn.academy.bank.product.application.ProductService;
import cn.academy.bank.product.domain.FinancialProduct;
import cn.academy.bank.shared.api.ApiResponse;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.validation.MoneyAmount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    public record SubscribeRequest(
            @Positive long productId,
            @NotBlank @Size(max = 36) String accountId,
            @MoneyAmount BigDecimal amount) {}

    @GetMapping
    ApiResponse<List<FinancialProduct>> list() {
        return ApiResponse.ok(service.products());
    }

    @PostMapping("/subscriptions")
    ApiResponse<ProductService.Subscription> subscribe(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody SubscribeRequest request) {
        return ApiResponse.ok(
                service.subscribe(
                        key,
                        new ProductService.Subscribe(
                                request.productId(), request.accountId(), request.amount())));
    }

    @GetMapping("/subscriptions")
    ApiResponse<PageResult<ProductService.Subscription>> subscriptions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.subscriptions(page, size));
    }
}
