package cn.academy.bank.audit.web;

import cn.academy.bank.audit.application.AuditQueryService;
import cn.academy.bank.shared.api.ApiResponse;
import cn.academy.bank.shared.api.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audits")
public class AuditController {
    private final AuditQueryService service;

    public AuditController(AuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PageResult<AuditQueryService.Audit>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.list(page, size));
    }
}
