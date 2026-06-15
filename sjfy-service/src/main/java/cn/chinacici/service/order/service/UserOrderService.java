package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.LoginRespDto;
import cn.chinacici.service.order.dto.SessionDto;

public interface UserOrderService {
    LoginRespDto login(String username, String password);
    void logout(String token);
    SessionDto getSession(String token);
    /** 从 token 获取 userId，token 无效则抛 NOT_LOGIN */
    Integer requireUserId(String token);
    /** 从 token 获取 session，非管理员（role > 1）则抛 USER_NO_PRIVILEGE */
    SessionDto requireAdmin(String token);
    /** 从 token 获取 session，非超级管理员（role != 0）则抛 USER_NO_PRIVILEGE */
    SessionDto requireSuperAdmin(String token);
}
