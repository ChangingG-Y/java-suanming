package cn.chinacici.service.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("lo_weight_record")
public class LoWeightRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer tenantId;
    private String recordDate;
    private java.math.BigDecimal weight;
    private String note;
    private Integer createdAt;
    private Integer updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getTenantId() { return tenantId; }
    public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }

    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }

    public java.math.BigDecimal getWeight() { return weight; }
    public void setWeight(java.math.BigDecimal weight) { this.weight = weight; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Integer getCreatedAt() { return createdAt; }
    public void setCreatedAt(Integer createdAt) { this.createdAt = createdAt; }

    public Integer getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Integer updatedAt) { this.updatedAt = updatedAt; }
}
