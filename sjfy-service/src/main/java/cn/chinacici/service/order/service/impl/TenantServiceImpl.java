package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.TenantDto;
import cn.chinacici.service.order.dto.TenantUserDto;
import cn.chinacici.service.order.entity.LoTenant;
import cn.chinacici.service.order.entity.LoUser;
import cn.chinacici.service.order.mapper.LoTenantMapper;
import cn.chinacici.service.order.mapper.LoUserMapper;
import cn.chinacici.service.order.service.TenantService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TenantServiceImpl implements TenantService {
    private final LoTenantMapper tenantMapper;
    private final LoUserMapper userMapper;

    public TenantServiceImpl(LoTenantMapper tenantMapper, LoUserMapper userMapper) {
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<TenantDto> listTenants() {
        List<LoTenant> tenants = tenantMapper.selectList(
            new LambdaQueryWrapper<LoTenant>().eq(LoTenant::getIsDeleted, 0)
        );
        return tenants.stream().map(t -> {
            TenantDto dto = new TenantDto();
            dto.setId(t.getId());
            dto.setName(t.getName());
            dto.setDescription(t.getDescription());
            long count = userMapper.selectCount(
                new LambdaQueryWrapper<LoUser>()
                    .eq(LoUser::getTenantId, t.getId())
                    .eq(LoUser::getIsDeleted, 0)
            );
            dto.setUserCount((int) count);
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public TenantDto createTenant(String name, String description) {
        if (!StringUtils.hasText(name)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "租户名称不能为空");
        }
        int now = (int)(System.currentTimeMillis() / 1000);
        LoTenant tenant = new LoTenant();
        tenant.setName(name);
        tenant.setDescription(description);
        tenant.setIsDeleted(0);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        tenantMapper.insert(tenant);
        TenantDto dto = new TenantDto();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setDescription(tenant.getDescription());
        dto.setUserCount(0);
        return dto;
    }

    @Override
    public List<TenantUserDto> listUsers(Integer tenantId) {
        List<LoUser> users = userMapper.selectList(
            new LambdaQueryWrapper<LoUser>()
                .eq(LoUser::getTenantId, tenantId)
                .eq(LoUser::getIsDeleted, 0)
        );
        return users.stream().map(this::toUserDto).collect(Collectors.toList());
    }

    @Override
    public TenantUserDto createUser(Integer tenantId, String username, String password, Integer role, String nickname) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "用户名和密码不能为空");
        }
        LoUser existing = userMapper.selectOne(
            new LambdaQueryWrapper<LoUser>().eq(LoUser::getUsername, username)
        );
        if (existing != null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "用户名已存在");
        }
        int now = (int)(System.currentTimeMillis() / 1000);
        LoUser user = new LoUser();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role != null ? role : 2);
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setIsDeleted(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return toUserDto(user);
    }

    @Override
    public void updateUserPassword(Integer userId, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "密码不能为空");
        }
        int now = (int)(System.currentTimeMillis() / 1000);
        userMapper.update(null, new LambdaUpdateWrapper<LoUser>()
            .eq(LoUser::getId, userId)
            .set(LoUser::getPassword, newPassword)
            .set(LoUser::getUpdatedAt, now));
    }

    @Override
    public void updateUserRole(Integer userId, Integer role) {
        int now = (int)(System.currentTimeMillis() / 1000);
        userMapper.update(null, new LambdaUpdateWrapper<LoUser>()
            .eq(LoUser::getId, userId)
            .set(LoUser::getRole, role)
            .set(LoUser::getUpdatedAt, now));
    }

    @Override
    public void deleteUser(Integer userId) {
        int now = (int)(System.currentTimeMillis() / 1000);
        userMapper.update(null, new LambdaUpdateWrapper<LoUser>()
            .eq(LoUser::getId, userId)
            .set(LoUser::getIsDeleted, 1)
            .set(LoUser::getUpdatedAt, now));
    }

    private TenantUserDto toUserDto(LoUser u) {
        TenantUserDto dto = new TenantUserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setNickname(u.getNickname());
        dto.setRole(u.getRole());
        dto.setTenantId(u.getTenantId());
        return dto;
    }
}
