package cn.chinacici.service.order.dto;

import java.math.BigDecimal;

public class CalendarDayDto {
    private String date;
    private boolean hasOrder;
    private BigDecimal weight;
    private boolean hasVisit;
    private boolean hasDiary;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean isHasOrder() { return hasOrder; }
    public void setHasOrder(boolean hasOrder) { this.hasOrder = hasOrder; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public boolean isHasVisit() { return hasVisit; }
    public void setHasVisit(boolean hasVisit) { this.hasVisit = hasVisit; }

    public boolean isHasDiary() { return hasDiary; }
    public void setHasDiary(boolean hasDiary) { this.hasDiary = hasDiary; }
}
