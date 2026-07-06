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

    private static final int MAX_INSTRUCTION_LENGTH = 500;

    /**
     * @param lines       OCR 识别出的原文行，按原顺序
     * @param apiKey      用户自己填写的 DeepSeek API Key
     * @param model       可选，不传用默认模型
     * @param instruction 可选，用户对本次翻译的额外要求/修正说明，比如指出某个词翻译错了
     * @return 与 lines 等长、一一对应的翻译结果；不需要翻译的（型号/纯数字/品牌名等）原样返回
     */
    public List<String> translate(List<OcrLine> lines, String apiKey, String model, String instruction) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请先填写 DeepSeek API Key");
        }
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        if (instruction != null && instruction.length() > MAX_INSTRUCTION_LENGTH) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "翻译要求太长，请精简后再试");
        }

        String usedModel = StringUtils.hasText(model) ? model.trim() : properties.getDefaultModel();
        String requestBody = buildRequestBody(lines, usedModel, instruction);

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

    private String buildRequestBody(List<OcrLine> lines, String model, String instruction) {
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

        String systemPrompt = "你是专业的图片本地化翻译助手。输入是一张图片里按行识别出的原文数组(JSON array)，数组下标从 0 开始。"
                + "这些原文是 OCR 自动识别的，可能存在识别错误，你需要一边翻译一边帮忙识别可疑的行。"
                + "请针对你能给出确定判断的每一行，输出一个 JSON 数组，数组元素是 {\"i\": 原文下标, \"t\": 对应内容}。"
                + "规则：1) 产品型号/编码(如 BT-CS-105)、纯数字、尺寸单位(mm/inch)、品牌名、网址、邮箱、电话号码——这些不需要翻译，直接跳过，不要出现在返回数组里，我们会保留原图这一处不做任何改动；"
                + "2) 表头、标题、说明文字、段落——t 填翻译成的简体中文，语气自然、专业；"
                + "3) 如果某一行明显是 OCR 识别错误产生的乱码/无意义碎片(比如圆形徽标外圈弯曲装饰字被拆散识别出的单个字母或短碎片、无法构成完整词语的片段)，或者读起来不像一个独立的标签/语句、而像是把好几个本该分属不同表格单元格或不同行的内容硬拼接在了一起(比如一行里同时混杂了好几个互不相关的型号、好几组规格数字，明显是表格里跨行/跨列的文字被识别错误地拼成了一行)——这两种情况都不要猜测硬翻，直接跳过这一条、不要出现在返回数组里，我们会保留原图这一处不做任何改动；"
                + "4) i 必须和原文数组下标严格一一对应，宁可少给(跳过没把握的行)也绝不能错位；这个数组不需要覆盖全部下标，也不需要保持原文出现的顺序；"
                + "5) 只输出 JSON 数组本身，不要任何解释文字，不要用 markdown 代码块包裹。";
        if (StringUtils.hasText(instruction)) {
            systemPrompt += "\n\n用户对本次翻译的额外要求（优先按这个来，可能是纠正上一次的翻译错误）：\n" + instruction.trim();
        }

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

        JsonNode arr;
        try {
            arr = objectMapper.readTree(jsonArrayText);
            if (!arr.isArray()) {
                throw new ServiceException(ResultCode.UNKNOWN_ERROR, "DeepSeek 返回内容不是数组");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析 DeepSeek 翻译结果失败，原始内容：{}", content);
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "无法解析 DeepSeek 返回的翻译结果");
        }

        // 按 {i,t} 里的显式下标 i 对应回原文行，而不是信任返回数组的顺序/长度与原文一一对应。
        // 批量翻译几十条 OCR 碎片时模型很容易漏翻/多翻一条，纯按位置对应会导致后面所有行整体
        // 错位、被画进错误的表格框里——之前网页翻译效果差、表格错位就是这个原因。
        // 缺失的下标保留 null，ImageRedrawService 会跳过不做任何改动（保留原图该处不动），
        // 好过错位覆盖别的行。
        String[] result = new String[expectedSize];
        int matched = 0;
        for (JsonNode node : arr) {
            int idx;
            String text;
            if (node.isObject() && node.hasNonNull("i")) {
                idx = node.path("i").asInt(-1);
                text = node.path("t").asText("");
            } else {
                // 兜底：万一模型没按 {i,t} 格式返回，退化为按已匹配到的条数顺序对应
                idx = matched;
                text = node.asText("");
            }
            if (idx >= 0 && idx < expectedSize) {
                result[idx] = text;
                matched++;
            }
        }
        if (matched < expectedSize) {
            log.warn("DeepSeek 翻译覆盖 {}/{} 行，缺失的行将保留原图不做修改", matched, expectedSize);
        }
        List<String> list = new ArrayList<>(expectedSize);
        for (String s : result) {
            list.add(s);
        }
        return list;
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
