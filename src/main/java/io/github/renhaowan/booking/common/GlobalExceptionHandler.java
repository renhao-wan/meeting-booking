package io.github.renhaowan.booking.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：业务异常返回对应 code，参数校验失败返回 400，未预期异常返回 500。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Object> handleBusiness(BusinessException e) {
        log.warn("业务异常 code={}, message={}", e.getCode(), e.getMessage());
        return new ApiResponse<>(e.getCode(), e.getMessage(), e.getData());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Object> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", detail);
        return ApiResponse.error(ErrorCode.INVALID_PARAM, detail);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ApiResponse<Object> handleUnreadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        // 请求体不可读（典型：时间/日期格式不符合契约）——返回明确 400，让调用方（含大模型）可自纠正
        String detail = e.getMostSpecificCause().getMessage();
        log.warn("请求体不可读: {}", detail);
        return ApiResponse.error(ErrorCode.INVALID_PARAM, "请求体格式错误：" + detail);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleUnexpected(Exception e) {
        log.error("未预期异常", e);
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR, "服务内部异常");
    }
}
