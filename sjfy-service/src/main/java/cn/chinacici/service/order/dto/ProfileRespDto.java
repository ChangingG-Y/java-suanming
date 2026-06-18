package cn.chinacici.service.order.dto;

import java.math.BigDecimal;

public class ProfileRespDto {
    private Integer userId;
    private String nickname;
    private String avatarUrl;
    private BigDecimal height;
    private String bio;
    private String birthday;
    private String bannerUrl;
    private BigDecimal currentWeight;
    private String currentWeightDate;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public BigDecimal getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(BigDecimal currentWeight) { this.currentWeight = currentWeight; }

    public String getCurrentWeightDate() { return currentWeightDate; }
    public void setCurrentWeightDate(String currentWeightDate) { this.currentWeightDate = currentWeightDate; }
}
