package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.LayoutConfigDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.service.TenantConfigService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class TenantConfigController {
    private final TenantConfigService tenantConfigService;
    private final UserOrderService userOrderService;

    public TenantConfigController(TenantConfigService tenantConfigService,
                                  UserOrderService userOrderService) {
        this.tenantConfigService = tenantConfigService;
        this.userOrderService = userOrderService;
    }

    /** GET /api/order/config/layout - 需要登录，返回当前租户布局配置 */
    @GetMapping("/config/layout")
    public ResponseData<LayoutConfigDto> getLayout(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(tenantConfigService.getLayoutConfig(session.getTenantId()));
    }

    /** PUT /api/order/admin/config/layout - 需要管理员，更新布局配置 */
    @PutMapping("/admin/config/layout")
    public ResponseData<Void> updateLayout(
        @RequestBody LayoutConfigDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto admin = userOrderService.requireAdmin(auth);
        tenantConfigService.updateLayoutConfig(admin.getTenantId(), dto);
        return ResponseData.success();
    }
}
