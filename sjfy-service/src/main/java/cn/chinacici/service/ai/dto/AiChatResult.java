package cn.chinacici.service.ai.dto;

import java.util.Map;

/**
 * DeepSeek 聊天结果。
 */
public class AiChatResult {
    private String answer;
    private String model;
    private Map<String, Object> usage;

    public AiChatResult() {
    }

    public AiChatResult(String answer, String model, Map<String, Object> usage) {
        this.answer = answer;
        this.model = model;
        this.usage = usage;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Map<String, Object> getUsage() {
        return usage;
    }

    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
}
