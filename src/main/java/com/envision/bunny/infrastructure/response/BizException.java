package com.envision.bunny.infrastructure.response;

import com.envision.bunny.infrastructure.util.MsgUtils;

import java.io.Serializable;

/**
 * 自定义的业务异常类,Biz是Business的通用缩写
 *
 * @author jingjing.dong
 * @since 2021/3/21-16:54
 */
public class BizException extends RuntimeException implements Serializable {
    private final ErrorCode errorCode;
    private transient final Object[] args;

    public BizException(ErrorCode _code, Object[] _args) {
        super("env biz exception", null, false, false);
        this.errorCode = _code;
        this.args = _args;
    }

    public BizException(ErrorCode _code) {
        this(_code, new Object[0]);
    }

    public BizException(ErrorCode _code, String _args) {
        this(_code, new Object[]{_args});
    }

    public BizException(ErrorCode _code, String _args1, String _args2) {
        this(_code, new Object[]{_args1, _args2});
    }

    public BizException(ErrorCode _code, String _args1, String _args2, String _args3) {
        this(_code, new Object[]{_args1, _args2, _args3});
    }

    @Override
    public String getMessage() {
        return MsgUtils.getMessage(errorCode.name(), args);
    }

    public int getCode() {
        return errorCode.getCode();
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
