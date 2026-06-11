package cn.chinacici.exception;

import cn.chinacici.core.ResultCode;

public class ServiceException extends RuntimeException {
    private ResultCode resultCode;
    private Integer code = ResultCode.UNKNOWN_ERROR.getCode();
    private String msg;
    private Object data;

    public ServiceException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public ServiceException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
        this.msg = msg;
        this.resultCode = resultCode;
    }

    public ServiceException(ResultCode resultCode, String msg, Object data) {
        super(msg);
        this.code = resultCode.getCode();
        this.msg = msg;
        this.resultCode = resultCode;
        this.data = data;
    }

    public ServiceException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public ServiceException(ResultCode resultCode) {
        super(resultCode.getUserMsg());
        this.code = resultCode.getCode();
        this.msg = resultCode.getUserMsg();
        this.resultCode = resultCode;
    }

    public ServiceException(ResultCode resultCode, Object data) {
        super(resultCode.getUserMsg());
        this.code = resultCode.getCode();
        this.msg = resultCode.getUserMsg();
        this.resultCode = resultCode;
        this.data = data;
    }

    public ServiceException(String msg, Exception e) {
        super(msg, e);
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public Object getData() {
        return data;
    }
}
