package cn.chinacici.service.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 算命 AI 相关配置。
 *
 * <p>配置来源是 Spring Boot 的 {@code application.yml}，便于本地、测试和生产环境覆盖。</p>
 */
@Component
@ConfigurationProperties(prefix = "suanming.ai")
public class SuanmingAiProperties {
    private String username;
    private String password;
    private long tokenTtlSeconds = 86400L;
    private String provider = "deepseek";
    private String deepseekBaseUrl;
    private String deepseekApiKey;
    private String volcengineBaseUrl;
    private String volcengineApiKey;
    private String model;
    private List<String> allowedProviders = Arrays.asList("deepseek", "doubao");
    private List<String> allowedModels = Arrays.asList(
            "deepseek-v4-pro",
            "deepseek-v4-flash",
            "doubao-seed-2-0-mini-260428",
            "doubao-seed-2-0-pro-260215",
            "doubao-seed-2-0-lite-260428"
    );
    private boolean thinkingEnabled = true;
    private String reasoningEffort = "high";
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 120000;
    private int maxTokens = 4096;
    private String systemPrompt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDeepseekBaseUrl() {
        return deepseekBaseUrl;
    }

    public void setDeepseekBaseUrl(String deepseekBaseUrl) {
        this.deepseekBaseUrl = deepseekBaseUrl;
    }

    public String getDeepseekApiKey() {
        return deepseekApiKey;
    }

    public void setDeepseekApiKey(String deepseekApiKey) {
        this.deepseekApiKey = deepseekApiKey;
    }

    public String getVolcengineBaseUrl() {
        return volcengineBaseUrl;
    }

    public void setVolcengineBaseUrl(String volcengineBaseUrl) {
        this.volcengineBaseUrl = volcengineBaseUrl;
    }

    public String getVolcengineApiKey() {
        return volcengineApiKey;
    }

    public void setVolcengineApiKey(String volcengineApiKey) {
        this.volcengineApiKey = volcengineApiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getAllowedProviders() {
        return allowedProviders;
    }

    public void setAllowedProviders(List<String> allowedProviders) {
        this.allowedProviders = allowedProviders;
    }

    public List<String> getAllowedModels() {
        return allowedModels;
    }

    public void setAllowedModels(List<String> allowedModels) {
        this.allowedModels = allowedModels;
    }

    public boolean isThinkingEnabled() {
        return thinkingEnabled;
    }

    public void setThinkingEnabled(boolean thinkingEnabled) {
        this.thinkingEnabled = thinkingEnabled;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
