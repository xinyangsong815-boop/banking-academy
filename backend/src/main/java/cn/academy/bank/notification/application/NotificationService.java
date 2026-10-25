package cn.academy.bank.notification.application;

import cn.academy.bank.identity.application.CurrentStaff;
import cn.academy.bank.notification.domain.BusinessCompleted;
import cn.academy.bank.shared.api.PageResult;
import cn.academy.bank.shared.domain.BusinessException;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final JdbcClient jdbc;
    private final CurrentStaff current;
    private final Clock clock;

    public NotificationService(JdbcClient jdbc, CurrentStaff current, Clock clock) {
        this.jdbc = jdbc;
        this.current = current;
        this.clock = clock;
    }

    public record Notice(
            long id,
            long userId,
            String title,
            String content,
            OffsetDateTime readAt,
            OffsetDateTime createdAt) {}

    /** 同步观察者参与发布方事务；写失败会使该业务事务回滚。 */
    @EventListener
    public void onCompleted(BusinessCompleted event) {
        jdbc.sql(
                        "INSERT INTO notification(user_id,title,content,created_at) VALUES (:user,:title,:content,:now)")
                .param("user", event.actorId())
                .param("title", event.title())
                .param("content", event.content())
                .param("now", OffsetDateTime.now(clock))
                .update();
    }

    public PageResult<Notice> list(int page, int size) {
        int offset = PageResult.offset(page, size);
        long user = current.get().id();
        var items =
                jdbc.sql(
                                "SELECT * FROM notification WHERE user_id=:user ORDER BY id DESC LIMIT :size OFFSET :offset")
                        .param("user", user)
                        .param("size", size)
                        .param("offset", offset)
                        .query(Notice.class)
                        .list();
        long count =
                jdbc.sql("SELECT COUNT(*) FROM notification WHERE user_id=:user")
                        .param("user", user)
                        .query(Long.class)
                        .single();
        return new PageResult<>(items, count, page, size);
    }

    public void markRead(long id) {
        int changed =
                jdbc.sql(
                                "UPDATE notification SET read_at=COALESCE(read_at,:now) WHERE id=:id AND user_id=:user")
                        .param("now", OffsetDateTime.now(clock))
                        .param("id", id)
                        .param("user", current.get().id())
                        .update();
        if (changed == 0) {
            throw BusinessException.notFound("通知");
        }
    }
}
