package cn.chinacici.app.controller.imgtranslate;

import cn.chinacici.core.ResponseData;
import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.imgtranslate.ImgTranslateService;
import cn.chinacici.service.imgtranslate.dto.TranslateTask;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * 图片文字翻译接口。整个翻译流程可能要几十秒（OCR + DeepSeek + 重绘），如果同步跑在一次请求里
 * 很容易被 Nginx/浏览器判定超时（504）。所以拆成三个接口：
 * <ol>
 *   <li>POST /translate 提交任务，立刻返回 taskId，不等待翻译完成</li>
 *   <li>GET /status/{taskId} 前端轮询这个查进度</li>
 *   <li>GET /download/{taskId} 翻译完成后下载结果图片</li>
 * </ol>
 * 前端下载完之后调用 DELETE /{taskId} 清理服务器上的临时文件。
 *
 * <p>DeepSeek API Key 由前端通过 {@code X-Deepseek-Api-Key} 请求头传入，仅在提交任务时使用一次，
 * 服务端不落库、不持久化。</p>
 */
@RestController
@RequestMapping("/imgtranslate")
public class ImgTranslateController {
    private final ImgTranslateService imgTranslateService;

    public ImgTranslateController(ImgTranslateService imgTranslateService) {
        this.imgTranslateService = imgTranslateService;
    }

    @PostMapping("/translate")
    public ResponseData<Map<String, String>> translate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "instruction", required = false) String instruction,
            @RequestHeader("X-Deepseek-Api-Key") String apiKey
    ) {
        String taskId = imgTranslateService.startTranslate(file, apiKey, model, instruction);
        Map<String, String> data = new HashMap<>();
        data.put("taskId", taskId);
        return ResponseData.success(data);
    }

    @GetMapping("/status/{taskId}")
    public ResponseData<Map<String, String>> status(@PathVariable String taskId) {
        TranslateTask task = imgTranslateService.getTask(taskId);
        Map<String, String> data = new HashMap<>();
        data.put("status", task.getStatus().name().toLowerCase());
        if (task.getStatus() == TranslateTask.Status.ERROR) {
            data.put("msg", task.getErrorMsg());
        }
        return ResponseData.success(data);
    }

    @GetMapping("/download/{taskId}")
    public ResponseEntity<byte[]> download(@PathVariable String taskId) throws IOException {
        TranslateTask task = imgTranslateService.getTask(taskId);
        if (task.getStatus() != TranslateTask.Status.DONE || task.getResultFile() == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "任务还没完成，暂时无法下载");
        }
        byte[] bytes = Files.readAllBytes(task.getResultFile().toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(bytes);
    }

    @DeleteMapping("/{taskId}")
    public ResponseData<Object> delete(@PathVariable String taskId) {
        imgTranslateService.deleteTask(taskId);
        return ResponseData.success();
    }
}
