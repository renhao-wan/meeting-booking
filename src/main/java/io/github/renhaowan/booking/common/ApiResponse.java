package io.github.renhaowan.booking.common;

/**
 * 统一响应体：{"code":0,"message":"ok","data":...}
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static final String OK_MESSAGE = "ok";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.OK, OK_MESSAGE, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(ErrorCode.OK, OK_MESSAGE, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
