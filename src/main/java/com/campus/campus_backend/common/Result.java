package com.campus.campus_backend.common;

import java.util.Map;
import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.OK.getCode();
        r.message = ErrorCode.OK.getMessage();
        r.data = data;
        return r;
    }

    public static <T> Result<T> okMessage(String message) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.OK.getCode();
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = message == null ? errorCode.getMessage() : message;
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    // 移除重复的方法，实现正确的 ok(String, T) 方法
    public static <T> Result<T> ok(String message, T data) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.OK.getCode();
        r.message = message;
        r.data = data;
        return r;
    }
}