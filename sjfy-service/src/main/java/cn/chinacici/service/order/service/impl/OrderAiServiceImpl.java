package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.ai.config.SuanmingAiProperties;
import cn.chinacici.service.order.dto.CalorieAdviceReqDto;
import cn.chinacici.service.order.dto.CalorieAdviceRespDto;
import cn.chinacici.service.order.dto.CartItemDto;
import cn.chinacici.service.order.service.OrderAiService;
import cn.chinacici.service.order.service.TenantConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderAiServiceImpl implements OrderAiService {
    private static final Logger log = LoggerFactory.getLogger(OrderAiServiceImpl.class);

    private final TenantConfigService tenantConfigService;
    private final SuanmingAiProperties aiProperties;
    private final RestTemplate restTemplate;

    public OrderAiServiceImpl(TenantConfigService tenantConfigService, SuanmingAiProperties aiProperties) {
        this.tenantConfigService = tenantConfigService;
        this.aiProperties = aiProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public CalorieAdviceRespDto getCalorieAdvice(CalorieAdviceReqDto req, Integer tenantId) {
        Integer tid = tenantId != null ? tenantId : 1;
        String enabled = tenantConfigService.getConfig(tid, "order.ai.enabled", "1");
        if (!"1".equals(enabled)) {
            return new CalorieAdviceRespDto(null, false);
        }
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            return new CalorieAdviceRespDto(null, true);
        }

        String provider = tenantConfigService.getConfig(tid, "order.ai.provider", "doubao");
        String model = tenantConfigService.getConfig(tid, "order.ai.model", "doubao-seed-2-0-lite-260428");
        String promptTemplate = tenantConfigService.getConfig(tid, "order.ai.prompt", "");

        StringBuilder menu = new StringBuilder();
        for (CartItemDto item : req.getItems()) {
            menu.append(item.getDishName()).append(" x").append(item.getQuantity());
            if (StringUtils.hasText(item.getRemark())) {
                menu.append("(").append(item.getRemark()).append(")");
            }
            menu.append("\n");
        }
        String userContent = promptTemplate.replace("{菜单}", menu.toString().trim());
        if (!StringUtils.hasText(userContent)) {
            userContent = "请估算以下菜单的热量并给出简短用餐建议（80字以内）：\n" + menu;
        }

        String baseUrl;
        String apiKey;
        if ("deepseek".equals(provider)) {
            baseUrl = aiProperties.getDeepseekBaseUrl();
            apiKey = tenantConfigService.getConfig(tid, "ai.deepseek_api_key", "");
        } else {
            baseUrl = aiProperties.getVolcengineBaseUrl();
            apiKey = tenantConfigService.getConfig(tid, "ai.doubao_api_key", "");
        }

        if (!StringUtils.hasText(apiKey)) {
            log.warn("点餐AI热量分析：{} API Key 未配置（租户{}）", provider, tid);
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "AI API Key 未配置，请在管理后台 AI配置 中填写");
        }

        try {
            String advice = callAi(baseUrl, apiKey, model, userContent, tid);
            return new CalorieAdviceRespDto(advice, true);
        } catch (Exception e) {
            log.warn("点餐AI热量分析调用失败（租户{}）: {}", tid, e.getMessage());
            return new CalorieAdviceRespDto("热量分析暂时不可用，请稍后再试", true);
        }
    }

    @Override
    public String generateDishDescription(String name, Integer tenantId) {
        Integer tid = tenantId != null ? tenantId : 1;
        String provider = tenantConfigService.getConfig(tid, "order.ai.provider", "doubao");
        String model = tenantConfigService.getConfig(tid, "order.ai.model", "doubao-seed-2-0-lite-260428");
        String baseUrl;
        String apiKey;
        if ("deepseek".equals(provider)) {
            baseUrl = aiProperties.getDeepseekBaseUrl();
            apiKey = tenantConfigService.getConfig(tid, "ai.deepseek_api_key", "");
        } else {
            baseUrl = aiProperties.getVolcengineBaseUrl();
            apiKey = tenantConfigService.getConfig(tid, "ai.doubao_api_key", "");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "AI API Key 未配置，请在AI配置页面填写");
        }
        String prompt = "帮我给菜品「" + name + "」写一句简短的中文介绍，要求：一句话，16字以内，口语化有食欲感，直接输出介绍内容不加引号";
        return callAi(baseUrl, apiKey, model, prompt, tid);
    }

    private String callAi(String baseUrl, String apiKey, String model, String userContent, Integer tid) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", userContent);
        messages.add(msg);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String url = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + "/chat/completions";
        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        return extractContent(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null) return null;
        Object choices = responseBody.get("choices");
        if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) return null;
        Object first = ((List<?>) choices).get(0);
        if (!(first instanceof Map)) return null;
        Object message = ((Map<?, ?>) first).get("message");
        if (!(message instanceof Map)) return null;
        Map<?, ?> msgMap = (Map<?, ?>) message;
        String content = toNonEmptyString(msgMap.get("content"));
        if (content != null) return content.length() > 200 ? content.substring(0, 200) : content;
        String reasoning = toNonEmptyString(msgMap.get("reasoning_content"));
        if (reasoning != null) return reasoning.length() > 200 ? reasoning.substring(0, 200) : reasoning;
        return null;
    }

    private String toNonEmptyString(Object obj) {
        if (obj == null) return null;
        String s = String.valueOf(obj).trim();
        return s.isEmpty() || "null".equals(s) ? null : s;
    }
}
