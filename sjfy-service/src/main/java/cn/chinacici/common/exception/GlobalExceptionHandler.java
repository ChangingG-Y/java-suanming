package cn.chinacici.common.exception;

import cn.chinacici.constant.HttpStatus;
import cn.chinacici.core.ResponseData;
import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseData<Object> exception(Exception e) {
        log.error("系统异常", e);
        return ResponseData.error(ResultCode.UNKNOWN_ERROR.getCode(), "系统繁忙,请稍后重试");
    }

    @ExceptionHandler(BindException.class)
    public ResponseData<Object> bindException(BindException e) {
        log.warn("参数绑定异常", e);
        return ResponseData.error(ResultCode.UNKNOWN_ERROR.getCode(), e.getAllErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseData<Object> noHandlerFoundException(NoHandlerFoundException e) {
        log.warn("404 not found", e);
        return ResponseData.error(ResultCode.PAGE_NOT_FOUND);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseData<Object> serviceException(ServiceException e) {
        log.warn("业务异常:", e);
        return ResponseData.error(e.getCode(), e.getMsg(), e.getData());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseData<Object> httpRequestMethodNotSupportedException(Exception e) {
        log.warn("该接口请求方法错误:", e);
        return ResponseData.error("接口请求方法错误,检查GET和POST或者路由参数是否缺失");
    }
}
