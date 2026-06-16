package cn.chinacici.service.order.dto;

public class AiConfigDto {
    private String enabled;
    private String provider;
    private String model;
    private String prompt;
    private String doubaoApiKey;
    private String deepseekApiKey;
    private String dishDescPrompt;

    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getDoubaoApiKey() { return doubaoApiKey; }
    public void setDoubaoApiKey(String doubaoApiKey) { this.doubaoApiKey = doubaoApiKey; }
    public String getDeepseekApiKey() { return deepseekApiKey; }
    public void setDeepseekApiKey(String deepseekApiKey) { this.deepseekApiKey = deepseekApiKey; }
    public String getDishDescPrompt() { return dishDescPrompt; }
    public void setDishDescPrompt(String dishDescPrompt) { this.dishDescPrompt = dishDescPrompt; }
}
