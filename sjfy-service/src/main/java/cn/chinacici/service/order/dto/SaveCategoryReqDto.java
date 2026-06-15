package cn.chinacici.service.order.dto;

public class SaveCategoryReqDto {
    private String name;
    private Integer seq;
    private Integer state;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }

    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
}
