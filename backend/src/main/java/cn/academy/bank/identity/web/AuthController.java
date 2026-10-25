package cn.academy.bank.identity.web;

import cn.academy.bank.identity.application.AuthService;
import cn.academy.bank.identity.application.CurrentStaff;
import cn.academy.bank.identity.domain.StaffPrincipal;
import cn.academy.bank.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CurrentStaff current;

    public AuthController(AuthService auth, CurrentStaff current) {
        this.auth = auth;
        this.current = current;
    }

    public record LoginRequest(
            @NotBlank @Size(max = 40) String username,
            @NotBlank @Size(max = 100) String password) {}

    public record Me(StaffPrincipal user, Set<String> permissions) {}

    @PostMapping("/login")
    ApiResponse<AuthService.LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(auth.login(request.username(), request.password()));
    }

    @GetMapping("/me")
    ApiResponse<Me> me() {
        var staff = current.get();
        return ApiResponse.ok(new Me(staff, staff.permissions()));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        auth.logout(authorization);
        return ApiResponse.ok(null);
    }
}
