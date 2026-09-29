package io.github.renhaowan.booking.common;

/**
 * 业务异常：携带业务错误码，可携带附加数据（如幂等冲突时的既有记录）。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    private final transient Object data;

    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    public BusinessException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }
}
