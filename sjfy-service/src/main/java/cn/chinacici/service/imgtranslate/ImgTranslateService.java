package cn.chinacici.service.imgtranslate;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.imgtranslate.config.ImgTranslateProperties;
import cn.chinacici.service.imgtranslate.dto.OcrLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/** 图片文字翻译总编排：OCR -> DeepSeek 翻译 -> 抹字重画。 */
@Service
public class ImgTranslateService {
    private static final Logger log = LoggerFactory.getLogger(ImgTranslateService.class);

    private final ImgTranslateProperties properties;
    private final OcrService ocrService;
    private final DeepSeekTranslateService translateService;
    private final ImageRedrawService redrawService;

    public ImgTranslateService(ImgTranslateProperties properties,
                                OcrService ocrService,
                                DeepSeekTranslateService translateService,
                                ImageRedrawService redrawService) {
        this.properties = properties;
        this.ocrService = ocrService;
        this.translateService = translateService;
        this.redrawService = redrawService;
    }

    public byte[] translateImage(MultipartFile file, String apiKey, String model) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请上传图片");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "图片太大，请压缩后再上传");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "只支持图片文件");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请先填写 DeepSeek API Key");
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("imgtranslate-", suffixOf(file.getOriginalFilename()));
            file.transferTo(tempFile);

            List<OcrLine> lines = ocrService.extractLines(tempFile);
            if (lines.isEmpty()) {
                log.info("图片未识别到任何文字，原样返回");
                return Files.readAllBytes(tempFile.toPath());
            }

            List<String> translations = translateService.translate(lines, apiKey, model);
            return redrawService.redraw(tempFile, lines, translations);
        } catch (IOException e) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "图片读取失败");
        } finally {
            if (tempFile != null) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    private String suffixOf(String originalFilename) {
        if (originalFilename == null) {
            return ".jpg";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : ".jpg";
    }
}
