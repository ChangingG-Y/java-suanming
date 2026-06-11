package cn.chinacici.app.dto;

import cn.chinacici.service.ai.dto.AiHistoryMessage;

import java.util.List;
import java.util.Map;

/**
 * AI 八字问答请求。
 */
public class AiChatRequestDto {
    private String question;
    private Map<String, Object> baziContext;
    private List<AiHistoryMessage> history;
    private String model;
    private Boolean thinkingEnabled;
    private String reasoningEffort;
    private String systemPrompt;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Map<String, Object> getBaziContext() {
        return baziContext;
    }

    public void setBaziContext(Map<String, Object> baziContext) {
        this.baziContext = baziContext;
    }

    public List<AiHistoryMessage> getHistory() {
        return history;
    }

    public void setHistory(List<AiHistoryMessage> history) {
        this.history = history;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getThinkingEnabled() {
        return thinkingEnabled;
    }

    public void setThinkingEnabled(Boolean thinkingEnabled) {
        this.thinkingEnabled = thinkingEnabled;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
