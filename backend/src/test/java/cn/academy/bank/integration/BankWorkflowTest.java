package cn.academy.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.academy.bank.identity.application.AuthService;
import cn.academy.bank.identity.domain.StaffPrincipal;
import cn.academy.bank.notification.domain.BusinessCompleted;
import cn.academy.bank.shared.domain.BusinessException;
import cn.academy.bank.transfer.application.TransferService;
import cn.academy.bank.transfer.domain.TransferChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BankWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcClient jdbc;
    @Autowired AuthService auth;
    @Autowired TransferService transfers;
    @Autowired FailureSwitch failureSwitch;
    private String adminToken;

    @BeforeEach
    void login() {
        adminToken = auth.login("admin", "Learn@123456").token();
    }

    @AfterEach
    void cleanupContext() {
        SecurityContextHolder.clearContext();
    }

    private String bearer() {
        return "Bearer " + adminToken;
    }

    private BigDecimal balance(String id) {
        return jdbc.sql("SELECT balance FROM bank_account WHERE id=:id")
                .param("id", id)
                .query(BigDecimal.class)
                .single();
    }

    private String body(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private Map<String, Object> transfer(String amount, String channel) {
        return Map.of(
                "sourceId",
                "A1001",
                "targetId",
                "A1002",
                "amount",
                amount,
                "channel",
                channel,
                "remark",
                "测试");
    }

    @Test
    @Order(1)
    void authenticationCorsAndAuthorizationAreEnforced() throws Exception {
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(
                        post("/api/auth/login")
                                .header("Origin", "http://127.0.0.1:5173")
                                .contentType("application/json")
                                .content(body(Map.of("username", "admin", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
        String auditor = auth.login("auditor", "Learn@123456").token();
        mvc.perform(
                        post("/api/transfers")
                                .header("Authorization", "Bearer " + auditor)
                                .header("Idempotency-Key", "deny-key-0001")
                                .contentType("application/json")
                                .content(body(transfer("10", "STANDARD"))))
                .andExpect(status().isForbidden());
        String teller = auth.login("teller", "Learn@123456").token();
        mvc.perform(get("/api/audits").header("Authorization", "Bearer " + teller))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    void transferReplayReturnsSameReceiptAndBooksOnlyOnce() throws Exception {
        BigDecimal before = balance("A1001");
        String payload = body(transfer("100.00", "INSTANT"));
        var first =
                mvc.perform(
                                post("/api/transfers")
                                        .header("Authorization", bearer())
                                        .header("Idempotency-Key", "replay-key-0001")
                                        .contentType("application/json")
                                        .content(payload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.fee").value("1.00"))
                        .andReturn();
        String firstId =
                mapper.readTree(first.getResponse().getContentAsString()).at("/data/id").asText();
        mvc.perform(
                        post("/api/transfers")
                                .header("Authorization", bearer())
                                .header("Idempotency-Key", "replay-key-0001")
                                .contentType("application/json")
                                .content(body(transfer("100", "INSTANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstId));
        assertThat(balance("A1001")).isEqualByComparingTo(before.subtract(new BigDecimal("101")));
        assertThat(balance("SYS-FEE")).isEqualByComparingTo("1.00");
        mvc.perform(
                        post("/api/transfers")
                                .header("Authorization", bearer())
                                .header("Idempotency-Key", "replay-key-0001")
                                .contentType("application/json")
                                .content(body(transfer("101", "INSTANT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    @Order(3)
    void invalidAndFailedTransfersLeaveNoPartialWrites() throws Exception {
        BigDecimal before = balance("A1001");
        long count = jdbc.sql("SELECT COUNT(*) FROM booking").query(Long.class).single();
        mvc.perform(
                        post("/api/transfers")
                                .header("Authorization", bearer())
                                .contentType("application/json")
                                .content(body(transfer("10", "STANDARD"))))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/transfers")
                                .header("Authorization", bearer())
                                .header("Idempotency-Key", "invalid-money-0001")
                                .contentType("application/json")
                                .content(body(transfer("1.001", "STANDARD"))))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/transfers")
                                .header("Authorization", bearer())
                                .header("Idempotency-Key", "limit-key-0001")
                                .contentType("application/json")
                                .content(body(transfer("20000.01", "STANDARD"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SINGLE_LIMIT"));
        assertThat(balance("A1001")).isEqualByComparingTo(before);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM booking").query(Long.class).single())
                .isEqualTo(count);
        assertThat(
                        jdbc.sql(
                                        "SELECT COUNT(*) FROM idempotent_request WHERE request_key='limit-key-0001'")
                                .query(Long.class)
                                .single())
                .isZero();
    }

    @Test
    @Order(4)
    void frozenAccountAndNonzeroClosureAreRejected() throws Exception {
        mvc.perform(
                        put("/api/accounts/A1003/status")
                                .header("Authorization", bearer())
                                .contentType("application/json")
                                .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NONZERO_BALANCE"));
        mvc.perform(
                        put("/api/accounts/A1003/status")
                                .header("Authorization", bearer())
                                .contentType("application/json")
                                .content("{\"status\":\"FROZEN\"}"))
                .andExpect(status().isOk());
        var request =
                Map.of(
                        "sourceId",
                        "A1003",
                        "targetId",
                        "A1002",
                        "amount",
                        "10",
                        "channel",
                        "STANDARD",
                        "remark",
                        "");
        mvc.perform(
                        post("/api/transfers")
                                .header("Authorization", bearer())
                                .header("Idempotency-Key", "frozen-key-0001")
                                .contentType("application/json")
                                .content(body(request)))
                .andExpect(status().isConflict());
        mvc.perform(
                        put("/api/accounts/A1003/status")
                                .header("Authorization", bearer())
                                .contentType("application/json")
                                .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    void loanRequiresDifferentReviewerAndDisbursesOnce() throws Exception {
        String tellerToken = auth.login("teller", "Learn@123456").token();
        String payload =
                body(
                        Map.of(
                                "customerId",
                                2,
                                "accountId",
                                "A1002",
                                "principal",
                                "10000.01",
                                "months",
                                12,
                                "repaymentMethod",
                                "EQUAL_INSTALLMENT",
                                "purpose",
                                "教学用途"));
        var response =
                mvc.perform(
                                post("/api/loans")
                                        .header("Authorization", "Bearer " + tellerToken)
                                        .contentType("application/json")
                                        .content(payload))
                        .andExpect(status().isOk())
                        .andReturn();
        String id =
                mapper.readTree(response.getResponse().getContentAsString())
                        .at("/data/id")
                        .asText();
        mvc.perform(
                        post("/api/loans/" + id + "/review")
                                .header("Authorization", bearer())
                                .contentType("application/json")
                                .content("{\"approved\":true,\"note\":\"教学复核通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("APPROVED"));
        BigDecimal before = balance("A1002");
        mvc.perform(post("/api/loans/" + id + "/disburse").header("Authorization", bearer()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/loans/" + id + "/disburse").header("Authorization", bearer()))
                .andExpect(status().isConflict());
        assertThat(balance("A1002")).isEqualByComparingTo(before.add(new BigDecimal("10000.01")));
        assertThat(
                        jdbc.sql("SELECT SUM(principal) FROM repayment_plan WHERE loan_id=:id")
                                .param("id", id)
                                .query(BigDecimal.class)
                                .single())
                .isEqualByComparingTo("10000.01");
        var own =
                mvc.perform(
                                post("/api/loans")
                                        .header("Authorization", bearer())
                                        .contentType("application/json")
                                        .content(payload))
                        .andExpect(status().isOk())
                        .andReturn();
        String ownId =
                mapper.readTree(own.getResponse().getContentAsString()).at("/data/id").asText();
        mvc.perform(
                        post("/api/loans/" + ownId + "/review")
                                .header("Authorization", bearer())
                                .contentType("application/json")
                                .content("{\"approved\":true,\"note\":\"本人复核\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAKER_CHECKER"));
    }

    @Test
    @Order(6)
    void subscriptionRiskAndQuotaAreCheckedAndReplayDoesNotDebitTwice() throws Exception {
        var mismatch = Map.of("productId", 3, "accountId", "A1003", "amount", "5000");
        mvc.perform(
                        post("/api/products/subscriptions")
                                .header("Authorization", bearer())
                                .header("Idempotency-Key", "risk-key-0001")
                                .contentType("application/json")
                                .content(body(mismatch)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RISK_MISMATCH"));
        BigDecimal before = balance("A1001");
        String valid = body(Map.of("productId", 1, "accountId", "A1001", "amount", "100"));
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/products/subscriptions")
                                    .header("Authorization", bearer())
                                    .header("Idempotency-Key", "subscribe-key-0001")
                                    .contentType("application/json")
                                    .content(valid))
                    .andExpect(status().isOk());
        }
        assertThat(balance("A1001")).isEqualByComparingTo(before.subtract(new BigDecimal("100")));
        assertThat(
                        jdbc.sql("SELECT remaining_quota FROM financial_product WHERE id=1")
                                .query(BigDecimal.class)
                                .single())
                .isEqualByComparingTo("499900");
    }

    @Test
    @Order(7)
    void concurrentTransfersSerializeAndCannotExceedDailyLimit() throws Exception {
        long adminId =
                jdbc.sql("SELECT id FROM staff_user WHERE username='admin'")
                        .query(Long.class)
                        .single();
        var executor = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<Boolean>>();
        BigDecimal before = balance("A1003");
        try {
            for (int i = 0; i < 4; i++) {
                final int index = i;
                futures.add(
                        executor.submit(
                                () -> {
                                    start.await();
                                    setStaff(new StaffPrincipal(adminId, "admin", "管理员", "ADMIN"));
                                    try {
                                        transfers.transfer(
                                                "concurrent-limit-" + index,
                                                new TransferService.Command(
                                                        "A1003",
                                                        "A1002",
                                                        new BigDecimal("20000"),
                                                        TransferChannel.STANDARD,
                                                        "并发测试"));
                                        return true;
                                    } catch (BusinessException exception) {
                                        assertThat(exception.code()).isEqualTo("DAILY_LIMIT");
                                        return false;
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                }));
            }
            start.countDown();
            int success = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(20, TimeUnit.SECONDS)) success++;
            }
            assertThat(success).isEqualTo(2);
            assertThat(balance("A1003"))
                    .isEqualByComparingTo(before.subtract(new BigDecimal("40000")));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Order(99)
    void everyBookingIsBalancedAndSnapshotMatchesLedger() {
        var unbalanced =
                jdbc.sql(
                                "SELECT booking_id FROM ledger_entry GROUP BY booking_id HAVING SUM(amount) <> 0")
                        .query(String.class)
                        .list();
        assertThat(unbalanced).isEmpty();
        var mismatches =
                jdbc.sql(
                                "SELECT a.id FROM bank_account a LEFT JOIN ledger_entry e ON e.account_id=a.id GROUP BY a.id,a.balance HAVING a.balance<>COALESCE(SUM(e.amount),0)")
                        .query(String.class)
                        .list();
        assertThat(mismatches).isEmpty();
        assertThat(
                        jdbc.sql("SELECT SUM(balance) FROM bank_account")
                                .query(BigDecimal.class)
                                .single())
                .isZero();
    }

    @Test
    @Order(9)
    void logoutRevokesSession() throws Exception {
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer()))
                .andExpect(status().isUnauthorized());
    }

    private void setStaff(StaffPrincipal staff) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                staff,
                                null,
                                staff.permissions().stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .toList()));
    }

    @Test
    @Order(10)
    void errorAfterPostingRollsBackBalancesLedgerOrderNoticeAndIdempotencyRow() {
        long adminId =
                jdbc.sql("SELECT id FROM staff_user WHERE username='admin'")
                        .query(Long.class)
                        .single();
        setStaff(new StaffPrincipal(adminId, "admin", "管理员", "ADMIN"));
        BigDecimal source = balance("A1001");
        BigDecimal target = balance("A1002");
        long bookings = jdbc.sql("SELECT COUNT(*) FROM booking").query(Long.class).single();
        long notices = jdbc.sql("SELECT COUNT(*) FROM notification").query(Long.class).single();
        failureSwitch.enabled.set(true);
        try {
            assertThatThrownBy(
                            () ->
                                    transfers.transfer(
                                            "rollback-after-posting",
                                            new TransferService.Command(
                                                    "A1001",
                                                    "A1002",
                                                    new BigDecimal("10"),
                                                    TransferChannel.STANDARD,
                                                    "故障注入")))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            failureSwitch.enabled.set(false);
        }
        assertThat(balance("A1001")).isEqualByComparingTo(source);
        assertThat(balance("A1002")).isEqualByComparingTo(target);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM booking").query(Long.class).single())
                .isEqualTo(bookings);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM notification").query(Long.class).single())
                .isEqualTo(notices);
        assertThat(
                        jdbc.sql(
                                        "SELECT COUNT(*) FROM idempotent_request WHERE request_key='rollback-after-posting'")
                                .query(Long.class)
                                .single())
                .isZero();
    }

    @Test
    @Order(11)
    void concurrentReplayCreatesOnlyOneOrder() throws Exception {
        long adminId =
                jdbc.sql("SELECT id FROM staff_user WHERE username='admin'")
                        .query(Long.class)
                        .single();
        BigDecimal source = balance("A1001");
        var executor = Executors.newFixedThreadPool(3);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<String>>();
        try {
            for (int i = 0; i < 3; i++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    start.await();
                                    setStaff(new StaffPrincipal(adminId, "admin", "管理员", "ADMIN"));
                                    try {
                                        return transfers
                                                .transfer(
                                                        "same-concurrent-key",
                                                        new TransferService.Command(
                                                                "A1001",
                                                                "A1002",
                                                                new BigDecimal("10"),
                                                                TransferChannel.STANDARD,
                                                                "同键并发"))
                                                .id();
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                }));
            }
            start.countDown();
            Set<String> ids = new HashSet<>();
            for (Future<String> future : futures) {
                ids.add(future.get(20, TimeUnit.SECONDS));
            }
            assertThat(ids).hasSize(1);
            assertThat(balance("A1001"))
                    .isEqualByComparingTo(source.subtract(new BigDecimal("10")));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Order(12)
    void notificationOwnershipAndPaginationAreEnforced() throws Exception {
        long noticeId =
                jdbc.sql(
                                "SELECT MIN(id) FROM notification WHERE user_id=(SELECT id FROM staff_user WHERE username='admin')")
                        .query(Long.class)
                        .single();
        String tellerToken = auth.login("teller", "Learn@123456").token();
        mvc.perform(
                        put("/api/notifications/" + noticeId + "/read")
                                .header("Authorization", "Bearer " + tellerToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/accounts?page=0").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/dashboard").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyVolumes.length()").value(7));
    }

    @Test
    @Order(13)
    void emptyAccountCannotPayAndFailureDoesNotCreateOrder() throws Exception {
        var opened =
                mvc.perform(
                                post("/api/accounts")
                                        .header("Authorization", bearer())
                                        .contentType("application/json")
                                        .content("{\"customerId\":1,\"name\":\"零余额测试账户\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        String source =
                mapper.readTree(opened.getResponse().getContentAsString()).at("/data/id").asText();
        long orders = jdbc.sql("SELECT COUNT(*) FROM transfer_order").query(Long.class).single();
        mvc.perform(
                        post("/api/transfers")
                                .header("Authorization", bearer())
                                .header("Idempotency-Key", "empty-account-key")
                                .contentType("application/json")
                                .content(
                                        body(
                                                Map.of(
                                                        "sourceId",
                                                        source,
                                                        "targetId",
                                                        "A1002",
                                                        "amount",
                                                        "1.00",
                                                        "channel",
                                                        "STANDARD",
                                                        "remark",
                                                        "余额不足测试"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
        assertThat(balance(source)).isZero();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM transfer_order").query(Long.class).single())
                .isEqualTo(orders);
    }

    @Test
    @Order(14)
    void insufficientProductQuotaDoesNotDebitAccount() throws Exception {
        BigDecimal before = balance("A1001");
        BigDecimal quota =
                jdbc.sql("SELECT remaining_quota FROM financial_product WHERE id=1")
                        .query(BigDecimal.class)
                        .single();
        jdbc.sql("UPDATE financial_product SET remaining_quota=50 WHERE id=1").update();
        try {
            mvc.perform(
                            post("/api/products/subscriptions")
                                    .header("Authorization", bearer())
                                    .header("Idempotency-Key", "quota-failure-key")
                                    .contentType("application/json")
                                    .content(
                                            body(
                                                    Map.of(
                                                            "productId",
                                                            1,
                                                            "accountId",
                                                            "A1001",
                                                            "amount",
                                                            "100.00"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"));
            assertThat(balance("A1001")).isEqualByComparingTo(before);
            assertThat(
                            jdbc.sql("SELECT remaining_quota FROM financial_product WHERE id=1")
                                    .query(BigDecimal.class)
                                    .single())
                    .isEqualByComparingTo("50");
        } finally {
            jdbc.sql("UPDATE financial_product SET remaining_quota=:quota WHERE id=1")
                    .param("quota", quota)
                    .update();
        }
    }

    @Test
    @Order(15)
    void expiredSessionCannotReadWorkspace() throws Exception {
        String hash = cn.academy.bank.shared.infrastructure.Hashing.sha256(adminToken);
        jdbc.sql("UPDATE auth_session SET expires_at=:expiry WHERE token_hash=:hash")
                .param("expiry", java.time.OffsetDateTime.now().minusDays(1))
                .param("hash", hash)
                .update();
        mvc.perform(get("/api/auth/me").header("Authorization", bearer()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @TestConfiguration
    static class FailureConfiguration {
        @Bean
        FailureSwitch failureSwitch() {
            return new FailureSwitch();
        }
    }

    static class FailureSwitch {
        final AtomicBoolean enabled = new AtomicBoolean(false);

        @EventListener
        public void fail(BusinessCompleted event) {
            if (enabled.get()) throw new IllegalStateException("教学测试：记账之后注入故障");
        }
    }
}
