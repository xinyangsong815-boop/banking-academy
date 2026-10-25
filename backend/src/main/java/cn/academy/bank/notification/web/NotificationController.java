package cn.academy.bank.notification.web;

import cn.academy.bank.notification.application.NotificationService;
import cn.academy.bank.shared.api.ApiResponse;
import cn.academy.bank.shared.api.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PageResult<NotificationService.Notice>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.list(page, size));
    }

    @PutMapping("/{id}/read")
    ApiResponse<Void> read(@PathVariable long id) {
        service.markRead(id);
        return ApiResponse.ok(null);
    }
}
