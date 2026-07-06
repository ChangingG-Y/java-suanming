package cn.chinacici.service.imgtranslate;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.imgtranslate.config.ImgTranslateProperties;
import cn.chinacici.service.imgtranslate.dto.OcrLine;
import cn.chinacici.service.imgtranslate.dto.TranslateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 图片文字翻译总编排：OCR -&gt; DeepSeek 翻译 -&gt; 抹字重画。
 *
 * <p>整个流程可能要几十秒，同步跑在一次 HTTP 请求里很容易被 Nginx/浏览器判定超时（504）。
 * 所以这里改成异步任务模式：提交后立刻返回 taskId，前端轮询状态，完成后单独走下载接口，
 * 下载完再调用删除接口清理服务器上的临时文件——跟 order/file 那套下载接口是同一个思路。</p>
 */
@Service
public class ImgTranslateService {
    private static final Logger log = LoggerFactory.getLogger(ImgTranslateService.class);
    /** 任务闲置超过这么久还没人来下载，后台自动清理，防止磁盘堆积临时文件 */
    private static final long TASK_TTL_MS = 30 * 60 * 1000L;

    private final ImgTranslateProperties properties;
    private final OcrService ocrService;
    private final DeepSeekTranslateService translateService;
    private final ImageRedrawService redrawService;

    private final Map<String, TranslateTask> tasks = new ConcurrentHashMap<>();
    private final java.util.concurrent.ExecutorService workerPool = Executors.newFixedThreadPool(4);
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public ImgTranslateService(ImgTranslateProperties properties,
                                OcrService ocrService,
                                DeepSeekTranslateService translateService,
                                ImageRedrawService redrawService) {
        this.properties = properties;
        this.ocrService = ocrService;
        this.translateService = translateService;
        this.redrawService = redrawService;
        cleanupExecutor.scheduleAtFixedRate(this::cleanupStaleTasks, 5, 5, TimeUnit.MINUTES);
    }

    /** 校验并保存上传文件，提交异步翻译任务，立刻返回 taskId（不等待翻译完成）。 */
    public String startTranslate(MultipartFile file, String apiKey, String model, String instruction) {
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

        File tempFile;
        try {
            tempFile = File.createTempFile("imgtranslate-src-", suffixOf(file.getOriginalFilename()));
            file.transferTo(tempFile);
        } catch (IOException e) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "图片保存失败");
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        TranslateTask task = new TranslateTask(taskId);
        tasks.put(taskId, task);

        workerPool.submit(() -> runTranslate(task, tempFile, apiKey, model, instruction));
        return taskId;
    }

    private void runTranslate(TranslateTask task, File tempFile, String apiKey, String model, String instruction) {
        try {
            List<OcrLine> lines = ocrService.extractLines(tempFile);
            byte[] resultBytes;
            if (lines.isEmpty()) {
                log.info("图片未识别到任何文字，原样返回，taskId={}", task.getId());
                resultBytes = Files.readAllBytes(tempFile.toPath());
            } else {
                // 低置信度/疑似装饰性乱码的行（常见于圆形徽标外圈弯曲文字被拆散识别出的碎片）
                // 直接跳过：不送去翻译、也不参与重绘擦除，原图这些地方保持不动。
                // 之前把这类碎片也丢给大模型翻译，模型会对着乱码硬猜出新的乱码文字画上去。
                List<OcrLine> toTranslate = new ArrayList<>();
                List<Integer> toTranslateIdx = new ArrayList<>();
                int skipped = 0;
                for (int i = 0; i < lines.size(); i++) {
                    OcrLine line = lines.get(i);
                    if (isLikelyNoise(line)) {
                        skipped++;
                        continue;
                    }
                    toTranslate.add(line);
                    toTranslateIdx.add(i);
                }
                if (skipped > 0) {
                    log.info("跳过 {} 行低置信度/疑似装饰噪声文字，taskId={}", skipped, task.getId());
                }

                List<String> translations = new ArrayList<>(java.util.Collections.nCopies(lines.size(), null));
                if (!toTranslate.isEmpty()) {
                    List<String> partial = translateService.translate(toTranslate, apiKey, model, instruction);
                    for (int i = 0; i < toTranslateIdx.size() && i < partial.size(); i++) {
                        translations.set(toTranslateIdx.get(i), partial.get(i));
                    }
                }
                resultBytes = redrawService.redraw(tempFile, lines, translations);
            }
            File resultFile = File.createTempFile("imgtranslate-out-", ".jpg");
            Files.write(resultFile.toPath(), resultBytes);
            task.markDone(resultFile);
        } catch (ServiceException e) {
            task.markError(e.getMsg());
        } catch (Exception e) {
            log.error("图片翻译任务失败，taskId={}", task.getId(), e);
            task.markError("翻译失败，请稍后重试");
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    /**
     * 判断一行 OCR 文字是否疑似噪声（低置信度识别错误，或圆形徽标外圈弯曲装饰字被拆散
     * 识别出的短碎片）。命中的行不送去翻译、也不参与擦除重绘，保留原图不动，
     * 好过让大模型对着乱码硬猜出新的乱码画上去。
     */
    private boolean isLikelyNoise(OcrLine line) {
        String text = line.getText() == null ? "" : line.getText().trim();
        String compact = text.replaceAll("[^\\p{L}\\p{N}]", "");
        if (compact.isEmpty()) {
            return true;
        }
        if (line.getConfidence() < properties.getMinOcrConfidence()) {
            return true;
        }
        // 短碎片（字母数字总数很少）即使过了普通置信度门槛，也更容易是巧合识别出的噪声，
        // 需要更高的置信度才采信——这类多是徽标里被拆散的单个字母/短词。
        if (compact.length() <= properties.getShortFragmentMaxLength()
                && line.getConfidence() < properties.getShortFragmentMinConfidence()) {
            return true;
        }
        return false;
    }

    public TranslateTask getTask(String taskId) {
        TranslateTask task = tasks.get(taskId);
        if (task == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "任务不存在或已过期");
        }
        return task;
    }

    /** 前端下载完成后调用，清理服务器上的临时结果文件。 */
    public void deleteTask(String taskId) {
        TranslateTask task = tasks.remove(taskId);
        if (task != null && task.getResultFile() != null) {
            //noinspection ResultOfMethodCallIgnored
            task.getResultFile().delete();
        }
    }

    private void cleanupStaleTasks() {
        long now = System.currentTimeMillis();
        tasks.entrySet().removeIf(entry -> {
            TranslateTask task = entry.getValue();
            boolean stale = now - task.getCreatedAt() > TASK_TTL_MS;
            if (stale && task.getResultFile() != null) {
                //noinspection ResultOfMethodCallIgnored
                task.getResultFile().delete();
            }
            return stale;
        });
    }

    @PreDestroy
    public void shutdown() {
        workerPool.shutdownNow();
        cleanupExecutor.shutdownNow();
    }

    private String suffixOf(String originalFilename) {
        if (originalFilename == null) {
            return ".jpg";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : ".jpg";
    }
}
