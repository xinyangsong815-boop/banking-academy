package cn.academy.bank.ledger.application;

import cn.academy.bank.ledger.domain.LedgerEntry;
import cn.academy.bank.shared.api.PageResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class LedgerQueryService {
    private final JdbcClient jdbc;

    public LedgerQueryService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<LedgerEntry> entries(String accountId, int page, int size) {
        int offset = PageResult.offset(page, size);
        var items =
                jdbc.sql(
                                """
                SELECT e.id,e.booking_id,e.account_id,e.amount,e.balance_after,b.kind,b.description,e.created_at
                FROM ledger_entry e JOIN booking b ON b.id=e.booking_id
                WHERE e.account_id=:account ORDER BY e.id DESC LIMIT :size OFFSET :offset
                """)
                        .param("account", accountId)
                        .param("size", size)
                        .param("offset", offset)
                        .query(LedgerEntry.class)
                        .list();
        long total =
                jdbc.sql("SELECT COUNT(*) FROM ledger_entry WHERE account_id=:id")
                        .param("id", accountId)
                        .query(Long.class)
                        .single();
        return new PageResult<>(items, total, page, size);
    }
}
