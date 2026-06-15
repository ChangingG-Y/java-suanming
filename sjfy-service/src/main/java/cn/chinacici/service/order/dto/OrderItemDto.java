package cn.chinacici.service.order.dto;

public class OrderItemDto {
    private Integer id;
    private Integer orderId;
    private Integer dishId;
    private String dishName;
    private Integer quantity;
    private Integer itemPrice;
    private String remark;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getDishId() { return dishId; }
    public void setDishId(Integer dishId) { this.dishId = dishId; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getItemPrice() { return itemPrice; }
    public void setItemPrice(Integer itemPrice) { this.itemPrice = itemPrice; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
