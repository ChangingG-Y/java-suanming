package cn.chinacici.service.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("lo_file")
public class LoFile {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String originalName;
    private String filePath;
    private String thumbnailPath;
    private Long fileSize;
    private String contentType;
    private Integer uploadBy;
    private Integer isDeleted;
    private String createBy;
    private String updateBy;
    private Integer updatedAt;
    private Integer createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getUploadBy() { return uploadBy; }
    public void setUploadBy(Integer uploadBy) { this.uploadBy = uploadBy; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }

    public Integer getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Integer updatedAt) { this.updatedAt = updatedAt; }

    public Integer getCreatedAt() { return createdAt; }
    public void setCreatedAt(Integer createdAt) { this.createdAt = createdAt; }
}
