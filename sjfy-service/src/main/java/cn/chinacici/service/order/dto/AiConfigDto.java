package cn.chinacici.service.order.dto;

/** 管理后台AI配置DTO */
public class AiConfigDto {
    private String enabled;   // "0"=关闭 "1"=开启
    private String provider;  // "doubao" or "deepseek"
    private String model;     // 模型ID
    private String prompt;    // 提示词模板,{菜单}会被替换

    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
