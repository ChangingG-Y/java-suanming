package cn.chinacici.service.order.dto;

public class DishRespDto {
    private Integer id;
    private Integer categoryId;
    private String name;
    private String description;
    private Integer imageFileId;
    private String imageUrl;
    private String thumbnailUrl;
    private Integer state;
    private Integer seq;
    private Integer price;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getImageFileId() { return imageFileId; }
    public void setImageFileId(Integer imageFileId) { this.imageFileId = imageFileId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }

    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
}
