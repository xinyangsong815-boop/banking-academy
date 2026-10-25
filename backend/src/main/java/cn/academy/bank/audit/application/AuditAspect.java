package cn.academy.bank.audit.application;

import cn.academy.bank.identity.application.CurrentStaff;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** 教学型操作审计：记录成功返回的操作名，不反射打印密码、令牌或客户参数。 */
@Aspect
@Component
@Order(300)
public class AuditAspect {
    private final JdbcClient jdbc;
    private final CurrentStaff current;
    private final Clock clock;

    public AuditAspect(JdbcClient jdbc, CurrentStaff current, Clock clock) {
        this.jdbc = jdbc;
        this.current = current;
        this.clock = clock;
    }

    @Around("@annotation(audited)")
    public Object record(ProceedingJoinPoint point, Audited audited) throws Throwable {
        Object result = point.proceed();
        try {
            jdbc.sql(
                            "INSERT INTO audit_log(actor,action,trace_id,created_at) VALUES (:actor,:action,:trace,:now)")
                    .param("actor", current.get().username())
                    .param("action", audited.value())
                    .param("trace", MDC.get("traceId"))
                    .param("now", OffsetDateTime.now(clock))
                    .update();
        } catch (Exception exception) {
            // 业务可能已提交，不能把审计写入失败伪装成业务失败；生产应接持久化 outbox。
            LoggerFactory.getLogger(AuditAspect.class).error("Audit persistence failed", exception);
        }
        return result;
    }
}
