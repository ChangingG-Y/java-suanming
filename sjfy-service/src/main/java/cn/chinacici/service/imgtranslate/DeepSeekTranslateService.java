package cn.chinacici.service.imgtranslate;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.imgtranslate.config.ImgTranslateProperties;
import cn.chinacici.service.imgtranslate.dto.OcrLine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用 DeepSeek 把整页 OCR 识别出的文字行批量翻译成中文。
 *
 * <p>API Key 由前端用户自己填写、随请求传过来，服务端只在本次调用中使用，不落库、不打日志。</p>
 */
@Service
public class DeepSeekTranslateService {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekTranslateService.class);
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[[\\s\\S]*]");

    private final ImgTranslateProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public DeepSeekTranslateService(ImgTranslateProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * @param lines  OCR 识别出的原文行，按原顺序
     * @param apiKey 用户自己填写的 DeepSeek API Key
     * @param model  可选，不传用默认模型
     * @return 与 lines 等长、一一对应的翻译结果；不需要翻译的（型号/纯数字/品牌名等）原样返回
     */
    public List<String> translate(List<OcrLine> lines, String apiKey, String model) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请先填写 DeepSeek API Key");
        }
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }

        String usedModel = StringUtils.hasText(model) ? model.trim() : properties.getDefaultModel();
        String requestBody = buildRequestBody(lines, usedModel);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        try {
            String url = trimTrailingSlash(properties.getDeepseekBaseUrl()) + "/chat/completions";
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), String.class);
            return parseTranslations(response.getBody(), lines.size());
        } catch (HttpStatusCodeException e) {
            log.warn("DeepSeek 调用失败，status={}, body={}", e.getRawStatusCode(), e.getResponseBodyAsString());
            if (e.getRawStatusCode() == 401) {
                throw new ServiceException(ResultCode.PARAMETER_ERROR, "DeepSeek API Key 无效或已过期");
            }
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 调用失败：" + summarize(e.getResponseBodyAsString()));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek 调用异常", e);
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "翻译服务暂时不可用，请稍后重试");
        }
    }

    private String buildRequestBody(List<OcrLine> lines, String model) {
        List<String> originalTexts = new ArrayList<>();
        for (OcrLine line : lines) {
            originalTexts.add(line.getText());
        }
        String linesJson;
        try {
            linesJson = objectMapper.writeValueAsString(originalTexts);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "原文序列化失败");
        }

        String systemPrompt = "你是专业的图片本地化翻译助手。输入是一张图片里按行识别出的英文原文数组(JSON array)，"
                + "请把每一行翻译成中文，输出一个**长度和顺序完全一致**的 JSON 字符串数组。"
                + "规则：1) 产品型号/编码(如 BT-CS-105)、纯数字、尺寸单位(mm/inch)、品牌名、网址、邮箱、电话号码——原样保留不翻译；"
                + "2) 表头、标题、说明文字、段落——翻译成简体中文，语气自然、专业；"
                + "3) 无法判断或看起来是噪声(单个符号/乱码)的，原样返回；"
                + "4) 只输出 JSON 数组本身，不要任何解释文字，不要用 markdown 代码块包裹。";

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", "原文数组：\n" + linesJson);

        List<Object> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        body.put("temperature", 0.3);

        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "请求体序列化失败");
        }
    }

    private List<String> parseTranslations(String responseBody, int expectedSize) {
        if (!StringUtils.hasText(responseBody)) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回为空");
        }
        String content;
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 未返回可用结果");
            }
            content = choices.get(0).path("message").path("content").asText("");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回格式解析失败");
        }

        if (!StringUtils.hasText(content)) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回内容为空");
        }

        Matcher matcher = JSON_ARRAY_PATTERN.matcher(content);
        String jsonArrayText = matcher.find() ? matcher.group() : content.trim();

        try {
            JsonNode arr = objectMapper.readTree(jsonArrayText);
            if (!arr.isArray()) {
                throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回内容不是数组");
            }
            List<String> result = new ArrayList<>();
            for (JsonNode node : arr) {
                result.add(node.asText(""));
            }
            if (result.size() != expectedSize) {
                log.warn("DeepSeek 返回条数({})与原文行数({})不一致", result.size(), expectedSize);
            }
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析 DeepSeek 翻译结果失败，原始内容：{}", content);
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "无法解析 DeepSeek 返回的翻译结果");
        }
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "https://api.deepseek.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String summarize(String body) {
        if (!StringUtils.hasText(body)) {
            return "无响应内容";
        }
        return body.length() > 240 ? body.substring(0, 240) + "..." : body;
    }
}
