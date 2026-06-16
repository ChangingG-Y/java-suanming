package cn.chinacici.service.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("lo_diary")
public class LoDiary {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer tenantId;
    private String diaryDate;
    private String content;
    private Integer createdAt;
    private Integer updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }

    public String getDiaryDate() { return diaryDate; }
    public void setDiaryDate(String diaryDate) { this.diaryDate = diaryDate; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getCreatedAt() { return createdAt; }
    public void setCreatedAt(Integer createdAt) { this.createdAt = createdAt; }

    public Integer getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Integer updatedAt) { this.updatedAt = updatedAt; }
}
