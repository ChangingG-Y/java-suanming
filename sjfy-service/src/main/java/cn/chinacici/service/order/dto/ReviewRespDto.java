package cn.chinacici.service.order.dto;

import java.util.List;

public class ReviewRespDto {
    private Integer id;
    private Integer orderId;
    private Integer userId;
    private Integer score;
    private String content;
    private List<FileRespDto> images;
    private Integer createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<FileRespDto> getImages() { return images; }
    public void setImages(List<FileRespDto> images) { this.images = images; }

    public Integer getCreatedAt() { return createdAt; }
    public void setCreatedAt(Integer createdAt) { this.createdAt = createdAt; }
}
