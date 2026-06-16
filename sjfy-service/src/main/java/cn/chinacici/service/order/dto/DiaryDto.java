package cn.chinacici.service.order.dto;

import java.util.List;

public class DiaryDto {
    private Integer id;
    private String diaryDate;
    private String content;
    private List<Integer> fileIds;
    private List<String> imageUrls;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getDiaryDate() { return diaryDate; }
    public void setDiaryDate(String diaryDate) { this.diaryDate = diaryDate; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<Integer> getFileIds() { return fileIds; }
    public void setFileIds(List<Integer> fileIds) { this.fileIds = fileIds; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}
