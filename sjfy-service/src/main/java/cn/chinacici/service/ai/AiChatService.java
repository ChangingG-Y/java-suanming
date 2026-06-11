package cn.chinacici.service.ai;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.ai.config.SuanmingAiProperties;
import cn.chinacici.service.ai.dto.AiChatResult;
import cn.chinacici.service.ai.dto.AiHistoryMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 八字 AI 问答服务。
 *
 * <p>服务层统一拼接命理系统提示词、完整八字上下文和用户问题，然后调用 DeepSeek V4。
 * 前端只负责收集排盘结果，不直接持有任何模型密钥。</p>
 */
@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MAX_QUESTION_LENGTH = 2000;

    private final SuanmingAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public AiChatService(SuanmingAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = createRestTemplate(properties);
    }

    /**
     * 调用 DeepSeek 完成一次八字问答。
     *
     * @param question 用户当前问题
     * @param baziContext 前端计算出的完整八字上下文
     * @param history 前端保留的最近聊天历史，可为空
     * @return AI 回答
     */
    public AiChatResult chat(String question,
                             Object baziContext,
                             List<AiHistoryMessage> history,
                             String requestedModel,
                             Boolean requestedThinkingEnabled,
                             String requestedReasoningEffort) {
        if (!StringUtils.hasText(question)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请输入想问 AI 的问题");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "问题太长，请缩短后再发送");
        }
        if (baziContext == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "缺少八字排盘上下文，请先完成排盘");
        }
        if (!StringUtils.hasText(properties.getDeepseekApiKey())) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek API Key 未配置");
        }

        String model = resolveModel(requestedModel);
        boolean thinkingEnabled = requestedThinkingEnabled == null ? properties.isThinkingEnabled() : requestedThinkingEnabled;
        String reasoningEffort = resolveReasoningEffort(requestedReasoningEffort);

        Map<String, Object> requestBody = buildDeepSeekRequest(question, baziContext, history, model, thinkingEnabled, reasoningEffort);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getDeepseekApiKey());

        try {
            String url = trimTrailingSlash(properties.getDeepseekBaseUrl()) + "/chat/completions";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), Map.class);
            return parseDeepSeekResponse(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("DeepSeek 调用失败，status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 调用失败：" + summarizeError(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("DeepSeek 调用异常", e);
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "AI 服务暂时不可用，请稍后重试");
        }
    }

    private Map<String, Object> buildDeepSeekRequest(String question,
                                                     Object baziContext,
                                                     List<AiHistoryMessage> history,
                                                     String model,
                                                     boolean thinkingEnabled,
                                                     String reasoningEffort) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(message("system", properties.getSystemPrompt()));

        if (!CollectionUtils.isEmpty(history)) {
            int fromIndex = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
            for (AiHistoryMessage historyMessage : history.subList(fromIndex, history.size())) {
                if (historyMessage == null || !StringUtils.hasText(historyMessage.getContent())) {
                    continue;
                }
                String role = normalizeRole(historyMessage.getRole());
                if (role == null) {
                    continue;
                }
                messages.add(message(role, historyMessage.getContent()));
            }
        }

        messages.add(message("user", buildUserContent(question, baziContext)));

        Map<String, Object> thinking = new HashMap<>();
        thinking.put("type", thinkingEnabled ? "enabled" : "disabled");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("thinking", thinking);
        if (thinkingEnabled) {
            requestBody.put("reasoning_effort", reasoningEffort);
        }
        requestBody.put("stream", false);
        requestBody.put("max_tokens", properties.getMaxTokens());
        return requestBody;
    }

    private String buildUserContent(String question, Object baziContext) {
        try {
            return "用户当前问题：\n" + question.trim()
                    + "\n\n前端完整八字排盘上下文 JSON：\n"
                    + objectMapper.writeValueAsString(baziContext);
        } catch (JsonProcessingException e) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "八字上下文序列化失败");
        }
    }

    private AiChatResult parseDeepSeekResponse(Map<String, Object> responseBody) {
        if (responseBody == null) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回为空");
        }
        Object choicesObj = responseBody.get("choices");
        if (!(choicesObj instanceof List) || ((List<?>) choicesObj).isEmpty()) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 未返回可用回答");
        }
        Object firstChoice = ((List<?>) choicesObj).get(0);
        if (!(firstChoice instanceof Map)) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回格式异常");
        }
        Object messageObj = ((Map<?, ?>) firstChoice).get("message");
        if (!(messageObj instanceof Map)) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回消息格式异常");
        }
        Object content = ((Map<?, ?>) messageObj).get("content");
        String answer = content == null ? "" : String.valueOf(content).trim();
        if (!StringUtils.hasText(answer)) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回内容为空");
        }

        Map<String, Object> usage = null;
        Object usageObj = responseBody.get("usage");
        if (usageObj instanceof Map) {
            usage = (Map<String, Object>) usageObj;
        }
        return new AiChatResult(answer, String.valueOf(responseBody.getOrDefault("model", properties.getModel())), usage);
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content == null ? "" : content);
        return message;
    }

    private String normalizeRole(String role) {
        if ("user".equals(role) || "assistant".equals(role)) {
            return role;
        }
        return null;
    }

    private String resolveModel(String requestedModel) {
        String model = StringUtils.hasText(requestedModel) ? requestedModel.trim() : properties.getModel();
        if (CollectionUtils.isEmpty(properties.getAllowedModels())) {
            return model;
        }
        if (!properties.getAllowedModels().contains(model)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "不支持的模型：" + model);
        }
        return model;
    }

    private String resolveReasoningEffort(String requestedReasoningEffort) {
        String effort = StringUtils.hasText(requestedReasoningEffort)
                ? requestedReasoningEffort.trim()
                : properties.getReasoningEffort();
        if ("xhigh".equals(effort)) {
            return "max";
        }
        if ("low".equals(effort) || "medium".equals(effort)) {
            return "high";
        }
        if (!"high".equals(effort) && !"max".equals(effort)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "不支持的思考强度：" + effort);
        }
        return effort;
    }

    private RestTemplate createRestTemplate(SuanmingAiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "https://api.deepseek.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String summarizeError(String errorBody) {
        if (!StringUtils.hasText(errorBody)) {
            return "接口无响应内容";
        }
        return errorBody.length() > 240 ? errorBody.substring(0, 240) + "..." : errorBody;
    }
}
