package cn.chinacici.service.imgtranslate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片文字翻译功能配置。
 *
 * <p>DeepSeek API Key 由前端用户自行输入、随请求带过来，服务端不落库、不持久化，
 * 只在本次请求生命周期内使用，因此这里不配置 apiKey。</p>
 */
@Component
@ConfigurationProperties(prefix = "suanming.imgtranslate")
public class ImgTranslateProperties {
    /** tesseract 可执行文件路径，留空则直接用 PATH 里的 tesseract */
    private String tesseractPath = "tesseract";
    /** OCR 语言包，需要服务器已安装对应 traineddata，例如 chi_sim+eng */
    private String ocrLang = "eng";
    /** 画中文用的字体文件路径（ttf/otf），默认是打包进 resources 的 Noto Sans CJK SC（SIL OFL 开源字体） */
    private String fontPath = "fonts/NotoSansCJKsc-Medium.otf";
    /** DeepSeek 接口地址 */
    private String deepseekBaseUrl = "https://api.deepseek.com";
    /** 默认模型 */
    private String defaultModel = "deepseek-v4-flash";
    /** 上传图片大小上限（字节） */
    private long maxFileSizeBytes = 15L * 1024 * 1024;
    /** tesseract 子进程超时（毫秒） */
    private int ocrTimeoutMs = 30000;
    /** DeepSeek 请求连接超时（毫秒） */
    private int connectTimeoutMs = 10000;
    /** DeepSeek 请求读取超时（毫秒），翻译整页文字可能要等久一点 */
    private int readTimeoutMs = 90000;
    /**
     * OCR 行平均置信度低于此值（0~100）时，判定为疑似噪声/无意义碎片，跳过翻译和重绘，
     * 保留原图该处不动。常见于圆形徽标外圈弯曲装饰字被拆散识别出的片段。
     */
    private double minOcrConfidence = 55d;
    /** 判定"短碎片"的字符数阈值（去掉标点空格后），短碎片需要更高置信度才采信 */
    private int shortFragmentMaxLength = 3;
    /** 短碎片专用的置信度门槛（通常比 minOcrConfidence 更高，因为短文本更容易是巧合识别出的噪声） */
    private double shortFragmentMinConfidence = 75d;

    public String getTesseractPath() {
        return tesseractPath;
    }

    public void setTesseractPath(String tesseractPath) {
        this.tesseractPath = tesseractPath;
    }

    public String getOcrLang() {
        return ocrLang;
    }

    public void setOcrLang(String ocrLang) {
        this.ocrLang = ocrLang;
    }

    public String getFontPath() {
        return fontPath;
    }

    public void setFontPath(String fontPath) {
        this.fontPath = fontPath;
    }

    public String getDeepseekBaseUrl() {
        return deepseekBaseUrl;
    }

    public void setDeepseekBaseUrl(String deepseekBaseUrl) {
        this.deepseekBaseUrl = deepseekBaseUrl;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getOcrTimeoutMs() {
        return ocrTimeoutMs;
    }

    public void setOcrTimeoutMs(int ocrTimeoutMs) {
        this.ocrTimeoutMs = ocrTimeoutMs;
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

    public double getMinOcrConfidence() {
        return minOcrConfidence;
    }

    public void setMinOcrConfidence(double minOcrConfidence) {
        this.minOcrConfidence = minOcrConfidence;
    }

    public int getShortFragmentMaxLength() {
        return shortFragmentMaxLength;
    }

    public void setShortFragmentMaxLength(int shortFragmentMaxLength) {
        this.shortFragmentMaxLength = shortFragmentMaxLength;
    }

    public double getShortFragmentMinConfidence() {
        return shortFragmentMinConfidence;
    }

    public void setShortFragmentMinConfidence(double shortFragmentMinConfidence) {
        this.shortFragmentMinConfidence = shortFragmentMinConfidence;
    }
}
