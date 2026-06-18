package cn.chinacici.service.order.dto;

import java.math.BigDecimal;

public class ProfileUpdateDto {
    private BigDecimal height;
    private String bio;
    private String nickname;
    private String birthday;

    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
}
