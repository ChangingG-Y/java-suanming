package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.TenantDto;
import cn.chinacici.service.order.dto.TenantUserDto;

import java.util.List;

public interface TenantService {
    List<TenantDto> listTenants();
    TenantDto createTenant(String name, String description);
    List<TenantUserDto> listUsers(Integer tenantId);
    TenantUserDto createUser(Integer tenantId, String username, String password, Integer role, String nickname);
    void updateUserPassword(Integer userId, String newPassword);
    void updateUserRole(Integer userId, Integer role);
    void deleteUser(Integer userId);
}
