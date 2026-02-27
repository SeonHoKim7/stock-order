package com.stockandorder.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_VIEW = "error/error";

    /**
     * 비즈니스 규칙 위반 (도메인 로직에서 발생)
     */
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e) {
        log.warn("[BusinessException] {} - {}", e.getErrorCode(), e.getMessage());
        return buildErrorView(e.getErrorCode().getStatus().value(), e.getMessage());
    }

    /**
     * @Valid 검증 실패 (컨트롤러에서 BindingResult 없이 쓸 경우 도달)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[ValidationException] {}", message);
        return buildErrorView(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 인가 실패 (Spring Security AccessDeniedException 안전망)
     * Spring Security의 ExceptionTranslationFilter가 먼저 처리하지만,
     * 서비스 레이어 등에서 직접 던진 경우 여기서 잡힌다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[AccessDeniedException] {}", e.getMessage());
        return buildErrorView(HttpStatus.FORBIDDEN.value(), ErrorCode.ACCESS_DENIED.getMessage());
    }

    /**
     * 존재하지 않는 URL/리소스 요청 (404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("[NoResourceFoundException] {}", e.getMessage());
        return buildErrorView(HttpStatus.NOT_FOUND.value(), "요청한 페이지를 찾을 수 없습니다.");
    }

    /**
     * 그 외 예상하지 못한 예외 (500)
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e) {
        log.error("[UnexpectedException]", e);
        return buildErrorView(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    private ModelAndView buildErrorView(int status, String message) {
        ModelAndView mav = new ModelAndView(ERROR_VIEW);
        mav.addObject("status", status);
        mav.addObject("message", message);
        mav.setStatus(HttpStatus.valueOf(status));
        return mav;
    }
}
