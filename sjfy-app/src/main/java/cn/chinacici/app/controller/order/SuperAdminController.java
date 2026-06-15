package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.TenantDto;
import cn.chinacici.service.order.dto.TenantUserDto;
import cn.chinacici.service.order.service.TenantService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/super")
public class SuperAdminController {
    private final TenantService tenantService;
    private final UserOrderService userOrderService;

    public SuperAdminController(TenantService tenantService, UserOrderService userOrderService) {
        this.tenantService = tenantService;
        this.userOrderService = userOrderService;
    }

    /** GET /api/order/super/tenants - 租户列表 */
    @GetMapping("/tenants")
    public ResponseData<List<TenantDto>> listTenants(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireSuperAdmin(auth);
        return ResponseData.success(tenantService.listTenants());
    }

    /** POST /api/order/super/tenants - 创建租户 */
    @PostMapping("/tenants")
    public ResponseData<TenantDto> createTenant(
        @RequestBody Map<String, String> body,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireSuperAdmin(auth);
        return ResponseData.success(tenantService.createTenant(body.get("name"), body.get("description")));
    }

    /** GET /api/order/super/tenants/{id}/users - 租户用户列表 */
    @GetMapping("/tenants/{id}/users")
    public ResponseData<List<TenantUserDto>> listUsers(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireSuperAdmin(auth);
        return ResponseData.success(tenantService.listUsers(id));
    }

    /** POST /api/order/super/tenants/{id}/users - 新增用户 */
    @PostMapping("/tenants/{id}/users")
    public ResponseData<TenantUserDto> createUser(
        @PathVariable Integer id,
        @RequestBody Map<String, Object> body,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireSuperAdmin(auth);
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String nickname = (String) body.get("nickname");
        Integer role = body.get("role") != null ? Integer.valueOf(body.get("role").toString()) : 2;
        return ResponseData.success(tenantService.createUser(id, username, password, role, nickname));
    }

    /** PUT /api/order/super/users/{id}/password - 修改密码 */
    @PutMapping("/users/{id}/password")
    public ResponseData<Void> updatePassword(
        @PathVariable Integer id,
        @RequestBody Map<String, String> body,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireSuperAdmin(auth);
        tenantService.updateUserPassword(id, body.get("password"));
        return ResponseData.success();
    }

    /** PUT /api/order/super/users/{id}/role - 修改角色 */
    @PutMapping("/users/{id}/role")
    public ResponseData<Void> updateRole(
        @PathVariable Integer id,
        @RequestBody Map<String, Integer> body,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireSuperAdmin(auth);
        tenantService.updateUserRole(id, body.get("role"));
        return ResponseData.success();
    }

    /** DELETE /api/order/super/users/{id} - 删除用户 */
    @DeleteMapping("/users/{id}")
    public ResponseData<Void> deleteUser(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireSuperAdmin(auth);
        tenantService.deleteUser(id);
        return ResponseData.success();
    }
}
