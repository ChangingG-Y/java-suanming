package cn.chinacici.service.order.dto;

import java.util.List;

public class DayOrderSummaryDto {
    private String date;
    private int totalKiss;
    private List<OrderRespDto> orders;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getTotalKiss() { return totalKiss; }
    public void setTotalKiss(int totalKiss) { this.totalKiss = totalKiss; }

    public List<OrderRespDto> getOrders() { return orders; }
    public void setOrders(List<OrderRespDto> orders) { this.orders = orders; }
}
