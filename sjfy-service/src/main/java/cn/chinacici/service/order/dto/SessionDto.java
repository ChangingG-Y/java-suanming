package cn.chinacici.service.order.dto;

public class SessionDto {
    private Integer userId;
    private Integer role;
    private String nickname;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
