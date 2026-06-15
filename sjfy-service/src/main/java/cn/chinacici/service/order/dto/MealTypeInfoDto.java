package cn.chinacici.service.order.dto;

public class MealTypeInfoDto {
    private Integer mealType;
    private String mealTypeName;
    private Boolean canAddDish;
    private Integer todayDinnerOrderId;

    public Integer getMealType() { return mealType; }
    public void setMealType(Integer mealType) { this.mealType = mealType; }

    public String getMealTypeName() { return mealTypeName; }
    public void setMealTypeName(String mealTypeName) { this.mealTypeName = mealTypeName; }

    public Boolean getCanAddDish() { return canAddDish; }
    public void setCanAddDish(Boolean canAddDish) { this.canAddDish = canAddDish; }

    public Integer getTodayDinnerOrderId() { return todayDinnerOrderId; }
    public void setTodayDinnerOrderId(Integer todayDinnerOrderId) { this.todayDinnerOrderId = todayDinnerOrderId; }
}
