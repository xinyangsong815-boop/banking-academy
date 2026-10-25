package cn.academy.bank.config;

import cn.academy.bank.identity.domain.SessionStore;
import cn.academy.bank.shared.infrastructure.Hashing;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {
    private final SessionStore sessions;
    private final Clock clock;

    public BearerTokenFilter(SessionStore sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && header.length() <= 256) {
            sessions.find(Hashing.sha256(header.substring(7)), clock.instant())
                    .ifPresent(
                            staff -> {
                                var authorities =
                                        staff.permissions().stream()
                                                .map(SimpleGrantedAuthority::new)
                                                .toList();
                                var authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                staff, null, authorities);
                                SecurityContextHolder.getContext()
                                        .setAuthentication(authentication);
                            });
        }
        chain.doFilter(request, response);
    }
}
