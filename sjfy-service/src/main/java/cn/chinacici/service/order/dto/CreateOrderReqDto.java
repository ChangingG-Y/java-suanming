package cn.chinacici.service.order.dto;

import java.util.List;

public class CreateOrderReqDto {
    private Integer mealType;
    private Integer isAddDish = 0;
    private Integer parentOrderId;
    private String remark;
    private String verifyWord;
    private List<CartItemDto> items;

    public Integer getMealType() { return mealType; }
    public void setMealType(Integer mealType) { this.mealType = mealType; }

    public Integer getIsAddDish() { return isAddDish; }
    public void setIsAddDish(Integer isAddDish) { this.isAddDish = isAddDish; }

    public Integer getParentOrderId() { return parentOrderId; }
    public void setParentOrderId(Integer parentOrderId) { this.parentOrderId = parentOrderId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getVerifyWord() { return verifyWord; }
    public void setVerifyWord(String verifyWord) { this.verifyWord = verifyWord; }

    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
}
