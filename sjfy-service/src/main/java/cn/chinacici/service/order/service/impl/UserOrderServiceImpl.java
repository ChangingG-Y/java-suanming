package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.LoginRespDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.entity.LoUser;
import cn.chinacici.service.order.mapper.LoUserMapper;
import cn.chinacici.service.order.service.UserOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserOrderServiceImpl implements UserOrderService {
    private final LoUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private static final String TOKEN_PREFIX = "love:order:token:";
    private static final long TOKEN_TTL_DAYS = 7;

    public UserOrderServiceImpl(LoUserMapper userMapper,
                                StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LoginRespDto login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请输入用户名和密码");
        }
        LoUser user = userMapper.selectOne(
            new LambdaQueryWrapper<LoUser>()
                .eq(LoUser::getUsername, username)
                .eq(LoUser::getIsDeleted, 0)
        );
        if (user == null || !Objects.equals(password, user.getPassword())) {
            throw new ServiceException(ResultCode.USER_NO_PRIVILEGE, "用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
            TOKEN_PREFIX + token,
            user.getId() + ":" + user.getRole(),
            TOKEN_TTL_DAYS, TimeUnit.DAYS
        );
        LoginRespDto resp = new LoginRespDto();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setRole(user.getRole());
        resp.setNickname(user.getNickname());
        return resp;
    }

    @Override
    public void logout(String token) {
        String normalized = normalizeToken(token);
        if (StringUtils.hasText(normalized)) {
            redisTemplate.delete(TOKEN_PREFIX + normalized);
        }
    }

    @Override
    public SessionDto getSession(String token) {
        String[] parts = resolveToken(token);
        SessionDto dto = new SessionDto();
        dto.setUserId(Integer.parseInt(parts[0]));
        dto.setRole(Integer.parseInt(parts[1]));
        LoUser user = userMapper.selectById(dto.getUserId());
        if (user != null) dto.setNickname(user.getNickname());
        return dto;
    }

    @Override
    public Integer requireUserId(String token) {
        String[] parts = resolveToken(token);
        return Integer.parseInt(parts[0]);
    }

    @Override
    public SessionDto requireAdmin(String token) {
        SessionDto session = getSession(token);
        if (session.getRole() == null || session.getRole() != 1) {
            throw new ServiceException(ResultCode.USER_NO_PRIVILEGE, "需要管理员权限");
        }
        return session;
    }

    private String[] resolveToken(String token) {
        String normalized = normalizeToken(token);
        if (!StringUtils.hasText(normalized)) {
            throw new ServiceException(ResultCode.NOT_LOGIN, "请先登录");
        }
        String value = redisTemplate.opsForValue().get(TOKEN_PREFIX + normalized);
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(ResultCode.NOT_LOGIN, "登录已过期，请重新登录");
        }
        return value.split(":");
    }

    private String normalizeToken(String authorization) {
        if (!StringUtils.hasText(authorization)) return "";
        String v = authorization.trim();
        return v.startsWith("Bearer ") ? v.substring(7).trim() : v;
    }
}
