package cn.academy.bank.report.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final JdbcClient jdbc;
    private final Clock clock;

    public ReportService(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public record DailyVolume(LocalDate date, long count, BigDecimal amount) {}

    public record Dashboard(
            long customers,
            long accounts,
            BigDecimal customerBalance,
            long pendingLoans,
            long transfers,
            BigDecimal transferredAmount,
            List<DailyVolume> dailyVolumes) {}

    public Dashboard dashboard() {
        var zone = ZoneId.of("Asia/Shanghai");
        var today = LocalDate.now(clock.withZone(zone));
        var daily = new java.util.ArrayList<DailyVolume>();
        for (int i = 6; i >= 0; i--) {
            var day = today.minusDays(i);
            var from = day.atStartOfDay(zone).toOffsetDateTime();
            var to = day.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
            daily.add(
                    jdbc.sql(
                                    "SELECT :day AS date,COUNT(*) AS count,COALESCE(SUM(amount),0) AS amount FROM transfer_order WHERE created_at>=:from AND created_at<:to")
                            .param("day", day)
                            .param("from", from)
                            .param("to", to)
                            .query(DailyVolume.class)
                            .single());
        }
        return new Dashboard(
                count("SELECT COUNT(*) FROM customer"),
                count("SELECT COUNT(*) FROM bank_account WHERE internal_account=FALSE"),
                decimal(
                        "SELECT COALESCE(SUM(balance),0) FROM bank_account WHERE internal_account=FALSE"),
                count("SELECT COUNT(*) FROM loan_application WHERE state='SUBMITTED'"),
                count("SELECT COUNT(*) FROM transfer_order"),
                decimal("SELECT COALESCE(SUM(amount),0) FROM transfer_order"),
                List.copyOf(daily));
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    private BigDecimal decimal(String sql) {
        return jdbc.sql(sql).query(BigDecimal.class).single();
    }
}
