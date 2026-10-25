package cn.academy.bank.report.web;

import cn.academy.bank.report.application.ReportService;
import cn.academy.bank.shared.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/health")
    ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "application", "banking-academy"));
    }

    @GetMapping("/dashboard")
    ApiResponse<ReportService.Dashboard> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }
}
