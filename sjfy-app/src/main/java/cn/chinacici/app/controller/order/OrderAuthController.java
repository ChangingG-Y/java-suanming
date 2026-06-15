package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.LoginReqDto;
import cn.chinacici.service.order.dto.LoginRespDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order/auth")
public class OrderAuthController {
    private final UserOrderService userOrderService;

    public OrderAuthController(UserOrderService userOrderService) {
        this.userOrderService = userOrderService;
    }

    /** POST /api/order/auth/login */
    @PostMapping("/login")
    public ResponseData<LoginRespDto> login(@RequestBody LoginReqDto dto) {
        return ResponseData.success(userOrderService.login(dto.getUsername(), dto.getPassword()));
    }

    /** POST /api/order/auth/logout */
    @PostMapping("/logout")
    public ResponseData<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        userOrderService.logout(auth);
        return ResponseData.success();
    }

    /** GET /api/order/auth/session */
    @GetMapping("/session")
    public ResponseData<SessionDto> session(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseData.success(userOrderService.getSession(auth));
    }
}
