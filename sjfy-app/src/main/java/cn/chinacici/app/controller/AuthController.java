package cn.chinacici.app.controller;

import cn.chinacici.app.dto.LoginRequestDto;
import cn.chinacici.core.ResponseData;
import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.auth.AuthService;
import cn.chinacici.service.auth.dto.SessionInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 助手登录接口。
 *
 * <p>固定账号只用于控制个人 AI 功能入口，不承担复杂用户体系职责。</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 固定账号登录，成功后返回 Bearer token。
     */
    @PostMapping("/login")
    public ResponseData<SessionInfo> login(@RequestBody LoginRequestDto requestDto) {
        if (requestDto == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请输入账号和密码");
        }
        return ResponseData.success(authService.login(requestDto.getUsername(), requestDto.getPassword()));
    }

    /**
     * 检查当前登录态是否仍然有效。
     */
    @GetMapping("/session")
    public ResponseData<SessionInfo> session(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseData.success(authService.requireSession(authorization));
    }
}
