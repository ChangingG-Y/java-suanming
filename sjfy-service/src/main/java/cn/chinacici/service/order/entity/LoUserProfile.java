package cn.chinacici.service.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("lo_user_profile")
public class LoUserProfile {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer tenantId;
    private java.math.BigDecimal height;
    private String bio;
    /** 生日，格式 YYYY-MM-DD，用于日历年度高亮 */
    private String birthday;
    private Integer createdAt;
    private Integer updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }

    public java.math.BigDecimal getHeight() { return height; }
    public void setHeight(java.math.BigDecimal height) { this.height = height; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public Integer getCreatedAt() { return createdAt; }
    public void setCreatedAt(Integer createdAt) { this.createdAt = createdAt; }

    public Integer getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Integer updatedAt) { this.updatedAt = updatedAt; }
}
