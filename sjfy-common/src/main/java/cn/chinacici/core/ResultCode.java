package cn.chinacici.core;

import cn.chinacici.constant.HttpStatus;

public enum ResultCode {
    UNKNOWN_ERROR(-1, "Unknown.Error", "系统繁忙,请稍后重试"),
    SUCCESS(0, "Success", "请求成功"),
    PAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "404 error", "页面没有找到,请检查您的访问链接"),

    ILLEGAL_REQUEST(11001, "Illegal.Request", "非法请求"),
    PARAMETER_ERROR(11002, "Parameter.Error", "参数错误"),
    DATA_NOT_FOUND(11201, "No.Data.Found", "未查询到数据"),
    NOT_LOGIN(11301, "Not.Login", "未登录"),
    USER_NO_PRIVILEGE(11309, "User.No.Privilege", "用户无权限操作");

    private final int code;
    private final String errMsg;
    private final String userMsg;

    ResultCode(int code, String errMsg, String userMsg) {
        this.code = code;
        this.errMsg = errMsg;
        this.userMsg = userMsg;
    }

    public int getCode() {
        return code;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public String getUserMsg() {
        return userMsg;
    }
}
