package cn.chinacici.app.controller.imgtranslate;

import cn.chinacici.core.ResponseData;
import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.imgtranslate.ImgTranslateService;
import cn.chinacici.service.imgtranslate.dto.PreviewLine;
import cn.chinacici.service.imgtranslate.dto.SuggestRequest;
import cn.chinacici.service.imgtranslate.dto.TranslateTask;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片文字翻译接口。整个翻译流程可能要几十秒（OCR + DeepSeek + 重绘），如果同步跑在一次请求里
 * 很容易被 Nginx/浏览器判定超时（504）。所以拆成几个接口：
 * <ol>
 *   <li>POST /translate 提交任务，立刻返回 taskId，不等待翻译完成</li>
 *   <li>GET /status/{taskId} 前端轮询这个查进度，跑完 OCR+翻译后状态会变成 review</li>
 *   <li>GET /preview/{taskId} status=review 时调用，拿每行坐标 + 原文 + AI 建议译文</li>
 *   <li>GET /source/{taskId} status=review 时调用，拿原图（还没擦字重画）给用户核对位置</li>
 *   <li>POST /suggest/{taskId} 用户手动挑一行，单独问 AI 要个翻译建议</li>
 *   <li>POST /confirm/{taskId} 用户看完/改完译文后调用，触发真正的擦字重画</li>
 *   <li>GET /download/{taskId} 重画完成（status=done）后下载结果图片</li>
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

    @GetMapping("/preview/{taskId}")
    public ResponseData<List<PreviewLine>> preview(@PathVariable String taskId) {
        return ResponseData.success(imgTranslateService.getPreview(taskId));
    }

    @GetMapping("/source/{taskId}")
    public ResponseEntity<byte[]> source(@PathVariable String taskId) throws IOException {
        File sourceFile = imgTranslateService.getSourceFileForPreview(taskId);
        byte[] bytes = Files.readAllBytes(sourceFile.toPath());
        String name = sourceFile.getName().toLowerCase();
        MediaType mediaType = name.endsWith(".png") ? MediaType.IMAGE_PNG
                : name.endsWith(".gif") ? MediaType.IMAGE_GIF
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(bytes);
    }

    /** 复核阶段用户手动挑一行，让 AI 单独给个翻译建议，不用自己想怎么翻。 */
    @PostMapping("/suggest/{taskId}")
    public ResponseData<Map<String, String>> suggest(
            @PathVariable String taskId,
            @RequestBody SuggestRequest req,
            @RequestHeader("X-Deepseek-Api-Key") String apiKey
    ) {
        String translated = imgTranslateService.suggestTranslation(taskId, req.getIndex(), apiKey, req.getModel(), req.getInstruction());
        Map<String, String> data = new HashMap<>();
        data.put("translated", translated);
        return ResponseData.success(data);
    }

    /** 请求体是一个 JSON 数组，跟 /preview 返回的行一一对应；某一项传 null/空字符串表示这一行保留原图不动。 */
    @PostMapping("/confirm/{taskId}")
    public ResponseData<Object> confirm(@PathVariable String taskId, @RequestBody List<String> translations) {
        imgTranslateService.confirmAndRender(taskId, translations);
        return ResponseData.success();
    }

    @GetMapping("/download/{taskId}")
    public ResponseEntity<byte[]> download(@PathVariable String taskId) throws IOException {
        TranslateTask task = imgTranslateService.getTask(taskId);
        if (task.getStatus() != TranslateTask.Status.DONE || task.getResultFile() == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "任务还没完成，暂时无法下载");
        }
        byte[] bytes = Files.readAllBytes(task.getResultFile().toPath());
        // 抹字重画的结果存的是无损 PNG，跳过重画（图片没识别到文字）时原样透传原图后缀，
        // 按实际文件后缀给 Content-Type，不能不管三七二十一都当 jpeg。
        MediaType mediaType = task.getResultFile().getName().toLowerCase().endsWith(".png")
                ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(bytes);
    }

    @DeleteMapping("/{taskId}")
    public ResponseData<Object> delete(@PathVariable String taskId) {
        imgTranslateService.deleteTask(taskId);
        return ResponseData.success();
    }
}
