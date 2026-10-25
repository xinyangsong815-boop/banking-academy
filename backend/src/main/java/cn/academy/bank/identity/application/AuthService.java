package cn.academy.bank.identity.application;

import cn.academy.bank.identity.domain.SessionStore;
import cn.academy.bank.identity.domain.StaffPrincipal;
import cn.academy.bank.shared.domain.BusinessException;
import cn.academy.bank.shared.infrastructure.Hashing;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final SessionStore sessions;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final long sessionHours;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;

    public AuthService(
            SessionStore sessions,
            PasswordEncoder encoder,
            Clock clock,
            @Value("${academy.session-hours}") long sessionHours) {
        this.sessions = sessions;
        this.encoder = encoder;
        this.clock = clock;
        this.sessionHours = sessionHours;
        this.dummyHash = encoder.encode("dummy-credential-never-used");
    }

    public record LoginResult(String token, Instant expiresAt, StaffPrincipal user) {}

    public LoginResult login(String username, String password) {
        var credential = sessions.credential(username);
        boolean matched =
                encoder.matches(
                        password,
                        credential.map(SessionStore.Credential::passwordHash).orElse(dummyHash));
        if (!matched || credential.isEmpty()) {
            throw new BusinessException("UNAUTHORIZED", "用户名或密码错误");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry = clock.instant().plus(sessionHours, ChronoUnit.HOURS);
        StaffPrincipal user = credential.orElseThrow().principal();
        sessions.create(Hashing.sha256(token), user.id(), expiry);
        return new LoginResult(token, expiry, user);
    }

    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            sessions.delete(Hashing.sha256(authorization.substring(7)));
        }
    }
}
