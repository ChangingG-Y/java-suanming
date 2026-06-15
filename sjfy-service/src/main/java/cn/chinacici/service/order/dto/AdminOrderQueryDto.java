package cn.chinacici.service.order.dto;

public class AdminOrderQueryDto {
    private String date;
    private Integer state;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
}
