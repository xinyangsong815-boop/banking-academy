package cn.academy.bank.identity.domain;

import java.time.Instant;
import java.util.Optional;

/** 身份模块的持久化端口；认证用例依赖能力，而非 JDBC 实现。 */
public interface SessionStore {
    record Credential(
            long id, String username, String passwordHash, String displayName, String role) {
        public StaffPrincipal principal() {
            return new StaffPrincipal(id, username, displayName, role);
        }
    }

    Optional<Credential> credential(String username);

    void create(String hash, long userId, Instant expiresAt);

    Optional<StaffPrincipal> find(String hash, Instant now);

    void delete(String hash);
}
