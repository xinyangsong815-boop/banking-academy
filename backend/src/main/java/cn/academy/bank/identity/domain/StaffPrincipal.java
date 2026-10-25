package cn.academy.bank.identity.domain;

import java.util.Set;

public record StaffPrincipal(long id, String username, String displayName, String role) {
    public Set<String> permissions() {
        return switch (role) {
            case "ADMIN" ->
                    Set.of(
                            "workspace:read",
                            "customer:write",
                            "account:write",
                            "transfer:write",
                            "loan:apply",
                            "loan:review",
                            "loan:disburse",
                            "product:write",
                            "audit:read");
            case "TELLER" ->
                    Set.of(
                            "workspace:read",
                            "customer:write",
                            "account:write",
                            "transfer:write",
                            "loan:apply",
                            "product:write");
            case "AUDITOR" -> Set.of("workspace:read", "audit:read");
            default -> Set.of();
        };
    }
}
