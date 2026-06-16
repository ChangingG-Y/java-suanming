package cn.chinacici.service.order.dto;

import java.util.List;

public class RestaurantVisitDto {
    private Integer id;
    private String visitDate;
    private Integer mealType;
    private String restaurantName;
    private Double score;
    private String content;
    private List<Integer> fileIds;
    private List<String> imageUrls;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }

    public Integer getMealType() { return mealType; }
    public void setMealType(Integer mealType) { this.mealType = mealType; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<Integer> getFileIds() { return fileIds; }
    public void setFileIds(List<Integer> fileIds) { this.fileIds = fileIds; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}
