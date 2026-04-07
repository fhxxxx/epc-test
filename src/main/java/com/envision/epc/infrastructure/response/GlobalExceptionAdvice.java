package com.envision.epc.infrastructure.response;

import com.envision.epc.infrastructure.util.MsgUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 所有的ExceptionHandler的返回值必须是Echo，否则会重复包装
 * @author jingjing.dong
 * @since 2021/3/21-16:44
 */
@SuppressWarnings("rawtypes")
@Slf4j(topic = "exception handler")
@RestControllerAdvice
public class GlobalExceptionAdvice {
    /**
     * 客户端中断异常
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e, Principal principal) {
        log.warn("ClientAbortException：{}",e.getMessage());
    }
    /**
     * 方法参数错误异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Echo handleMethodArgumentNotValidException(MethodArgumentNotValidException e, Principal principal) {
        List<String> msgList=new ArrayList<>();
        // 从异常对象中拿到ObjectError对象
        if (!e.getBindingResult().getAllErrors().isEmpty()){
            for(ObjectError error:e.getBindingResult().getAllErrors()){
                msgList.add(Objects.requireNonNull(error.getDefaultMessage()));
            }
        }
        final String msgStr = String.join(";", msgList);
        log.warn("用户输入错误:[{}]",msgStr);
        // 然后提取错误提示信息进行返回
        return new Echo<>(HttpStatus.BAD_REQUEST.value(), msgStr);
    }
    /**
     * 方法参数错误异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Echo handleConstraintViolationException(ConstraintViolationException exception) {
        List<String> msgList=new ArrayList<>();
        String strFormat = "[%s]数据错误,错误值:[%s],原因:[%s]";
        exception.getConstraintViolations().forEach(error -> {
            String message = error.getMessage();
            String value = Optional.ofNullable(error.getInvalidValue()).map(Object::toString).orElse("null");
            String path = error.getPropertyPath().toString();
            String errorStr = String.format(strFormat,path,value,message);
            msgList.add(errorStr);
        });
        // 然后提取错误提示信息进行返回
        final String msgStr = String.join(";", msgList);
        log.warn("用户输入错误:[{}]",msgStr);
        // 然后提取错误提示信息进行返回
        return new Echo<>(HttpStatus.BAD_REQUEST.value(), msgStr);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({BizException.class})
    public Echo handleServiceException(BizException e, HttpServletRequest request, HttpServletResponse response) {
        log.warn("系统自定义业务异常:[{}]",e.getMessage());
        return new Echo<>(e.getCode(),e.getMessage());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({AccessDeniedException.class})
    public Echo handleAccessDeniedExceptio(AccessDeniedException e, HttpServletRequest request, HttpServletResponse response) {
        return new Echo<>(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
    /**
     * 对内部的未定义的异常，均视为不应出现的异常，打印堆栈
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({Exception.class})
    public Echo handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        if (e.getCause() instanceof BizException) {
            final BizException bizException = (BizException)e.getCause();
            log.warn(bizException.getMessage());
            return new Echo<>(bizException.getCode(), bizException.getMessage());
        } else {
            log.error("Uncaught exception:", e);
            return new Echo<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), MsgUtils.getMessage("SYS_ERROR"));
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({MissingServletRequestParameterException.class})
    public Echo handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request, HttpServletResponse response) {
        return new Echo<>(HttpStatus.BAD_REQUEST.value(),MsgUtils.getMessage("REQUIRED_PARAMETER_IS_NOT_PRESENT"));
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public Echo handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request, HttpServletResponse response) {
        return new Echo<>(HttpStatus.BAD_REQUEST.value(),MsgUtils.getMessage("PARAMETER_PARSING_FAILED"));
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({BindException.class})
    public Echo handleBindException(BindException e, HttpServletRequest request, HttpServletResponse response) {
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        String field = Objects.requireNonNull(error).getField();
        String code = error.getDefaultMessage();
        String message = String.format("%s:%s", field, code);
        return new Echo<>(HttpStatus.BAD_REQUEST.value(),message);
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public Echo handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request, HttpServletResponse response) {
        return new Echo<>(HttpStatus.BAD_REQUEST.value(),HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class})
    public Echo handleHttpMediaTypeNotSupportedException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        return new Echo<>(HttpStatus.BAD_REQUEST.value(),HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({DataIntegrityViolationException.class})
    public Echo handleException(DataIntegrityViolationException e, HttpServletRequest request, HttpServletResponse response) {
        log.error("数据完整性冲突异常:[{}]",e.getMessage());
        return new Echo<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),MsgUtils.getMessage(                "DATA_INTEGRITY_VIOLATION_EXCEPTION"));
    }
}
