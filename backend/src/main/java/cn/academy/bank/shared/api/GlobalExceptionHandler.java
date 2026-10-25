package cn.academy.bank.shared.api;

import cn.academy.bank.shared.domain.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> business(BusinessException exception) {
        HttpStatus status =
                switch (exception.code()) {
                    case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
                    case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
                    case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
                    default -> HttpStatus.CONFLICT;
                };
        return ResponseEntity.status(status)
                .body(ApiResponse.error(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiResponse<Void>> validation(BindException exception) {
        String message =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .findFirst()
                        .orElse("参数不合法");
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        HandlerMethodValidationException.class,
        ServletRequestBindingException.class
    })
    ResponseEntity<ApiResponse<Void>> badRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", "请求参数格式不正确"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> missingRoute() {
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", "接口不存在"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> unsupportedMethod() {
        return ResponseEntity.status(405).body(ApiResponse.error("METHOD_NOT_ALLOWED", "请求方法不支持"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> unsupportedMedia() {
        return ResponseEntity.status(415)
                .body(ApiResponse.error("UNSUPPORTED_MEDIA_TYPE", "请使用 application/json"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> forbidden() {
        return ResponseEntity.status(403).body(ApiResponse.error("FORBIDDEN", "没有执行此操作的权限"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> conflict() {
        return ResponseEntity.status(409)
                .body(ApiResponse.error("DATA_CONFLICT", "数据冲突，请检查唯一字段或刷新后重试"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        LOG.error("Unhandled request failure", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "服务暂时不可用，请携带 traceId 排查"));
    }
}
