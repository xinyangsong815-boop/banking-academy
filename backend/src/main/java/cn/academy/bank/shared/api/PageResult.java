package cn.academy.bank.shared.api;

import java.util.List;

public record PageResult<T>(List<T> items, long total, int page, int size) {
    public PageResult {
        items = List.copyOf(items);
    }

    public static int offset(int page, int size) {
        if (page < 1 || page > 100_000 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page 应为 1~100000，size 应为 1~100");
        }
        return (page - 1) * size;
    }
}
