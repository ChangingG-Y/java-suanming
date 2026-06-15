package cn.chinacici.service.order.dto;

public class CategoryRespDto {
    private Integer id;
    private String name;
    private Integer seq;
    private Integer state;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }

    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }
}
