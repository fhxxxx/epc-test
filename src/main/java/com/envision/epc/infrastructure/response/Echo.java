package com.envision.epc.infrastructure.response;

import com.envision.epc.infrastructure.util.MsgUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author jingjing.dong
 * @since 2021/3/21-16:47
 */
@Setter
@Getter
@ToString
public
class Echo<T> {
    private T data;
    private int code;
    private String msg;


    public Echo() {
    }
    public Echo(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Echo(T data, int code, String msg) {
        this.data = data;
        this.code = code;
        this.msg = msg;
    }

    public static <T> Echo<T> success(T data) {
        return new Echo<>(data, 0, "");
    }

    public static <T> Echo<T> success(String msg) {
        return new Echo<>(null, 0, msg);
    }

    public static Echo<String> fail(ErrorCode code, String... variables) {
        return new Echo<>(code.getCode(), MsgUtils.getMessage(code.name(), variables));
    }
}
