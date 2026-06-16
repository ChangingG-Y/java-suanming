package cn.chinacici.service.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("lo_diary_image")
public class LoDiaryImage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer diaryId;
    private Integer fileId;
    private Integer sort;
    private Integer createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getDiaryId() { return diaryId; }
    public void setDiaryId(Integer diaryId) { this.diaryId = diaryId; }

    public Integer getFileId() { return fileId; }
    public void setFileId(Integer fileId) { this.fileId = fileId; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public Integer getCreatedAt() { return createdAt; }
    public void setCreatedAt(Integer createdAt) { this.createdAt = createdAt; }
}
