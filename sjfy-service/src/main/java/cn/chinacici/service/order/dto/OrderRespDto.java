package cn.chinacici.service.order.dto;

import java.util.List;

public class OrderRespDto {
    private Integer id;
    private String orderNo;
    private Integer userId;
    private String orderDate;
    private Integer mealType;
    private String mealTypeName;
    private Integer isAddDish;
    private Integer parentOrderId;
    private Integer state;
    private String stateName;
    private String remark;
    private List<OrderItemDto> items;
    private Boolean hasReview;
    private ReviewRespDto review;
    private Integer createdAt;
    // 变更3/5：总亲亲数
    private Integer totalKiss;
    // 变更5/6：评价分数（有评价时填score，无则null）
    private Integer reviewScore;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public Integer getMealType() { return mealType; }
    public void setMealType(Integer mealType) { this.mealType = mealType; }

    public String getMealTypeName() { return mealTypeName; }
    public void setMealTypeName(String mealTypeName) { this.mealTypeName = mealTypeName; }

    public Integer getIsAddDish() { return isAddDish; }
    public void setIsAddDish(Integer isAddDish) { this.isAddDish = isAddDish; }

    public Integer getParentOrderId() { return parentOrderId; }
    public void setParentOrderId(Integer parentOrderId) { this.parentOrderId = parentOrderId; }

    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public Boolean getHasReview() { return hasReview; }
    public void setHasReview(Boolean hasReview) { this.hasReview = hasReview; }

    public ReviewRespDto getReview() { return review; }
    public void setReview(ReviewRespDto review) { this.review = review; }

    public Integer getCreatedAt() { return createdAt; }
    public void setCreatedAt(Integer createdAt) { this.createdAt = createdAt; }

    public Integer getTotalKiss() { return totalKiss; }
    public void setTotalKiss(Integer totalKiss) { this.totalKiss = totalKiss; }

    public Integer getReviewScore() { return reviewScore; }
    public void setReviewScore(Integer reviewScore) { this.reviewScore = reviewScore; }
}
