package cn.chinacici.app.controller.imgtranslate;

import cn.chinacici.service.imgtranslate.ImgTranslateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片文字翻译接口。
 *
 * <p>DeepSeek API Key 由前端通过 {@code X-Deepseek-Api-Key} 请求头传入，仅本次请求使用，
 * 服务端不保存。返回值直接是翻译后的图片二进制，前端可以直接当 blob 下载。</p>
 */
@RestController
@RequestMapping("/imgtranslate")
public class ImgTranslateController {
    private final ImgTranslateService imgTranslateService;

    public ImgTranslateController(ImgTranslateService imgTranslateService) {
        this.imgTranslateService = imgTranslateService;
    }

    @PostMapping("/translate")
    public ResponseEntity<byte[]> translate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "model", required = false) String model,
            @RequestHeader("X-Deepseek-Api-Key") String apiKey
    ) {
        byte[] result = imgTranslateService.translateImage(file, apiKey, model);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result);
    }
}
