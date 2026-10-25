package cn.academy.bank.identity.infrastructure;

import cn.academy.bank.identity.domain.SessionStore;
import cn.academy.bank.identity.domain.StaffPrincipal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository implements SessionStore {
    private final JdbcClient jdbc;

    public SessionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Credential> credential(String username) {
        return jdbc.sql(
                        "SELECT id, username, password_hash, display_name, role FROM staff_user WHERE username=:name AND enabled=TRUE")
                .param("name", username)
                .query(Credential.class)
                .optional();
    }

    public void create(String hash, long userId, Instant expiresAt) {
        jdbc.sql(
                        "INSERT INTO auth_session(token_hash,user_id,expires_at) VALUES (:hash,:user,:expiry)")
                .param("hash", hash)
                .param("user", userId)
                .param("expiry", expiresAt.atOffset(ZoneOffset.UTC))
                .update();
    }

    public Optional<StaffPrincipal> find(String hash, Instant now) {
        return jdbc.sql(
                        """
                SELECT u.id,u.username,u.display_name,u.role FROM auth_session s
                JOIN staff_user u ON u.id=s.user_id
                WHERE s.token_hash=:hash AND s.expires_at>:now AND u.enabled=TRUE
                """)
                .param("hash", hash)
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .query(StaffPrincipal.class)
                .optional();
    }

    public void delete(String hash) {
        jdbc.sql("DELETE FROM auth_session WHERE token_hash=:hash").param("hash", hash).update();
    }
}
