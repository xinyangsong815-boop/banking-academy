package cn.academy.bank.shared.domain;

/** 领域异常不依赖 HTTP、Spring 或数据库。 */
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static BusinessException notFound(String name) {
        return new BusinessException("NOT_FOUND", name + "不存在");
    }

    public static void require(boolean condition, String code, String message) {
        if (!condition) {
            throw new BusinessException(code, message);
        }
    }
}
