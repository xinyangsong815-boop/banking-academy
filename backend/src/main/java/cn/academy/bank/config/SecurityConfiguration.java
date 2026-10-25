package cn.academy.bank.config;

import cn.academy.bank.shared.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            BearerTokenFilter bearer,
            ObjectMapper mapper,
            @Value("${academy.cors-origins}") String origins)
            throws Exception {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(
                java.util.Arrays.stream(origins.split(",")).map(String::strip).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        cors.setExposedHeaders(List.of("X-Trace-Id"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return http.cors(config -> config.configurationSource(source))
                // 浏览器不会自动附带 Bearer 头；此示例不使用 Cookie 身份凭据。
                .csrf(config -> config.disable())
                .sessionManagement(
                        config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        config ->
                                config.requestMatchers(HttpMethod.POST, "/api/auth/login")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        config ->
                                config.authenticationEntryPoint(
                                                (request, response, exception) -> {
                                                    response.setStatus(401);
                                                    response.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    mapper.writeValue(
                                                            response.getWriter(),
                                                            ApiResponse.error(
                                                                    "UNAUTHORIZED", "登录已失效，请重新登录"));
                                                })
                                        .accessDeniedHandler(
                                                (request, response, exception) -> {
                                                    response.setStatus(403);
                                                    response.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    mapper.writeValue(
                                                            response.getWriter(),
                                                            ApiResponse.error(
                                                                    "FORBIDDEN", "没有访问权限"));
                                                }))
                .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** 防止 @Component Filter 同时被 Servlet 容器和安全链注册两次。 */
    @Bean
    FilterRegistrationBean<BearerTokenFilter> bearerRegistration(BearerTokenFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
