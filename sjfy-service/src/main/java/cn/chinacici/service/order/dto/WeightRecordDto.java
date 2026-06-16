package cn.chinacici.service.order.dto;

import java.math.BigDecimal;

public class WeightRecordDto {
    private Integer id;
    private String recordDate;
    private BigDecimal weight;
    private String note;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
