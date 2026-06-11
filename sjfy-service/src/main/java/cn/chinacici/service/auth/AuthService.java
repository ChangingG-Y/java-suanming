package cn.chinacici.service.auth;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.ai.config.SuanmingAiProperties;
import cn.chinacici.service.auth.dto.SessionInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 固定账号登录与内存会话服务。
 *
 * <p>当前项目是个人工具，不引入数据库。重启后 token 会失效，用户重新登录即可。</p>
 */
@Service
public class AuthService {
    private final SuanmingAiProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    public AuthService(SuanmingAiProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验固定账号密码，成功后签发内存 token。
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录态信息
     */
    public SessionInfo login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请输入账号和密码");
        }
        if (!properties.getUsername().equals(username) || !properties.getPassword().equals(password)) {
            throw new ServiceException(ResultCode.USER_NO_PRIVILEGE, "账号或密码错误");
        }

        cleanupExpiredSessions();
        long now = System.currentTimeMillis();
        long expireAt = now + properties.getTokenTtlSeconds() * 1000L;
        String token = createToken();
        SessionInfo sessionInfo = new SessionInfo(token, username, expireAt, properties.getTokenTtlSeconds());
        sessionMap.put(token, sessionInfo);
        return sessionInfo;
    }

    /**
     * 校验 token，失败时抛出未登录业务异常。
     *
     * @param token 请求头里的 Bearer token
     * @return 有效登录态
     */
    public SessionInfo requireSession(String token) {
        String normalizedToken = normalizeBearerToken(token);
        if (!StringUtils.hasText(normalizedToken)) {
            throw new ServiceException(ResultCode.NOT_LOGIN, "请先登录 AI 助手");
        }

        SessionInfo sessionInfo = sessionMap.get(normalizedToken);
        if (sessionInfo == null || sessionInfo.getExpireAt() == null || sessionInfo.getExpireAt() < System.currentTimeMillis()) {
            sessionMap.remove(normalizedToken);
            throw new ServiceException(ResultCode.NOT_LOGIN, "登录态已过期，请重新登录");
        }

        long remainSeconds = Math.max(0L, (sessionInfo.getExpireAt() - System.currentTimeMillis()) / 1000L);
        return new SessionInfo(normalizedToken, sessionInfo.getUsername(), sessionInfo.getExpireAt(), remainSeconds);
    }

    /**
     * 宽松解析 Authorization 请求头，兼容直接传 token 和 Bearer token 两种形式。
     */
    public String normalizeBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return "";
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        return value;
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, SessionInfo>> iterator = sessionMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SessionInfo> entry = iterator.next();
            SessionInfo value = entry.getValue();
            if (value == null || value.getExpireAt() == null || value.getExpireAt() < now) {
                iterator.remove();
            }
        }
    }
}
