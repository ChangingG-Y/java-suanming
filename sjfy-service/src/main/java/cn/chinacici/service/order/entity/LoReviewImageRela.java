package cn.chinacici.service.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("lo_review_image_rela")
public class LoReviewImageRela {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer reviewId;
    private Integer fileId;
    private Integer seq;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }

    public Integer getFileId() { return fileId; }
    public void setFileId(Integer fileId) { this.fileId = fileId; }

    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }
}
