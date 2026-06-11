package cn.chinacici.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一响应数据。
 */
public class ResponseData<T> {
    private Integer code;
    private String msg;
    private T data;
    private static final Map<String, Object> DEFAULT_MAP = new HashMap<>(0);

    public static ResponseData success(Integer code, String msg, Object data) {
        ResponseData responseData = new ResponseData();
        responseData.setCode(code);
        responseData.setMsg(msg);
        responseData.setData(data);
        return responseData;
    }

    public static ResponseData success(Object data) {
        return success(ResultCode.SUCCESS.getCode(), "success", data == null ? DEFAULT_MAP : data);
    }

    public static ResponseData success(String msg) {
        return success(ResultCode.SUCCESS.getCode(), msg, DEFAULT_MAP);
    }

    public static ResponseData success() {
        return success(ResultCode.SUCCESS.getCode(), "success", DEFAULT_MAP);
    }

    public static ResponseData error() {
        return success(ResultCode.UNKNOWN_ERROR.getCode(), "error", DEFAULT_MAP);
    }

    public static ResponseData error(String msg) {
        return success(ResultCode.UNKNOWN_ERROR.getCode(), msg, DEFAULT_MAP);
    }

    public static ResponseData error(Integer code, String msg) {
        return success(code, msg, DEFAULT_MAP);
    }

    public static ResponseData error(Integer code, String msg, Object data) {
        return success(code, msg, data == null ? DEFAULT_MAP : data);
    }

    public static ResponseData error(ResultCode resultCode) {
        return success(resultCode.getCode(), resultCode.getUserMsg(), DEFAULT_MAP);
    }

    public static ResponseData error(ResultCode resultCode, String msg) {
        return success(resultCode.getCode(), msg, DEFAULT_MAP);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
