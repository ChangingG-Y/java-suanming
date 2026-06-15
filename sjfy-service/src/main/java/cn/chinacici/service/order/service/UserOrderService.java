package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.LoginRespDto;
import cn.chinacici.service.order.dto.SessionDto;

public interface UserOrderService {
    LoginRespDto login(String username, String password);
    void logout(String token);
    SessionDto getSession(String token);
    /** 从 token 获取 userId，如果 token 无效抛 ServiceException(NOT_LOGIN) */
    Integer requireUserId(String token);
    /** 从 token 获取用户信息，如果非管理员(role!=1)抛 ServiceException(USER_NO_PRIVILEGE) */
    SessionDto requireAdmin(String token);
}
