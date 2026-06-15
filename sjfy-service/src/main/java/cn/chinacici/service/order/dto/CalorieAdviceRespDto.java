package cn.chinacici.service.order.dto;

public class CalorieAdviceRespDto {
    /** AI 生成的热量估算+用餐建议（约80字以内） */
    private String advice;
    /** AI 功能是否开启 */
    private Boolean enabled;

    public CalorieAdviceRespDto() {}
    public CalorieAdviceRespDto(String advice, Boolean enabled) {
        this.advice = advice;
        this.enabled = enabled;
    }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
