package cn.chinacici.service.auth.dto;

/**
 * 前端登录态信息。
 */
public class SessionInfo {
    private String token;
    private String username;
    private Long expireAt;
    private Long expireInSeconds;

    public SessionInfo() {
    }

    public SessionInfo(String token, String username, Long expireAt, Long expireInSeconds) {
        this.token = token;
        this.username = username;
        this.expireAt = expireAt;
        this.expireInSeconds = expireInSeconds;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }

    public Long getExpireInSeconds() {
        return expireInSeconds;
    }

    public void setExpireInSeconds(Long expireInSeconds) {
        this.expireInSeconds = expireInSeconds;
    }
}
