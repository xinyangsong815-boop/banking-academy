package cn.academy.bank.shared.api;

import org.slf4j.MDC;

/** HTTP 状态表达协议结果，code 表达业务语义，traceId 用于关联日志。 */
public record ApiResponse<T>(String code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "操作成功", data, MDC.get("traceId"));
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, MDC.get("traceId"));
    }
}
