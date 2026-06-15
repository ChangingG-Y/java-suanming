package cn.chinacici.service.order.dto;

import java.util.List;

public class CreateReviewReqDto {
    private Integer orderId;
    private Integer score;
    private String content;
    private List<Integer> fileIds;

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<Integer> getFileIds() { return fileIds; }
    public void setFileIds(List<Integer> fileIds) { this.fileIds = fileIds; }
}
