package cn.chinacici.service.order.dto;

import java.util.List;

public class CalorieAdviceReqDto {
    private List<CartItemDto> items;

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
}
