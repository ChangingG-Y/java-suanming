package cn.chinacici.service.imgtranslate;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.imgtranslate.config.ImgTranslateProperties;
import cn.chinacici.service.imgtranslate.dto.OcrLine;
import cn.chinacici.service.imgtranslate.dto.PreviewLine;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 图片文字翻译总编排：OCR -&gt; DeepSeek 翻译 -&gt; 人工复核确认 -&gt; 抹字重画。
 *
 * <p>整个流程可能要几十秒，同步跑在一次 HTTP 请求里很容易被 Nginx/浏览器判定超时（504）。
 * 所以这里改成异步任务模式：提交后立刻返回 taskId，前端轮询状态。</p>
 *
 * <p>OCR + AI 翻译再准，也难免有翻错、翻漏、位置识别错位这些问题（人眼一眼能看出来，
 * 模型和几何规则不一定能防住），所以不再是"翻完直接烧进图里"：OCR+翻译跑完先进入
 * {@code REVIEW} 状态，把建议译文和坐标交给前端给用户看一遍、改一改，用户确认后才真正
 * 擦字重画，转 {@code DONE}，再走下载接口，下载完调用删除接口清理服务器上的临时文件——
 * 跟 order/file 那套下载接口是同一个思路。</p>
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

        workerPool.submit(() -> runOcrAndTranslate(task, tempFile, apiKey, model, instruction));
        return taskId;
    }

    /** 第一阶段：OCR + AI 翻译，跑完进入 REVIEW 状态等用户确认；没识别到文字就直接原样返回，没什么可复核的。 */
    private void runOcrAndTranslate(TranslateTask task, File tempFile, String apiKey, String model, String instruction) {
        try {
            List<OcrLine> lines = ocrService.extractLines(tempFile);
            if (lines.isEmpty()) {
                log.info("图片未识别到任何文字，原样返回，taskId={}", task.getId());
                byte[] resultBytes = Files.readAllBytes(tempFile.toPath());
                File resultFile = File.createTempFile("imgtranslate-out-", ".jpg");
                Files.write(resultFile.toPath(), resultBytes);
                task.markDone(resultFile);
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
                return;
            }

            // 低置信度/疑似装饰性乱码的行（常见于圆形徽标外圈弯曲文字被拆散识别出的碎片），
            // 以及疑似跨表格列/单元格拼接错行的行，直接跳过：不送去翻译，原图这些地方保持不动。
            List<OcrLine> toTranslate = new ArrayList<>();
            List<Integer> toTranslateIdx = new ArrayList<>();
            int skipped = 0;
            for (int i = 0; i < lines.size(); i++) {
                OcrLine line = lines.get(i);
                if (isLikelyNoise(line) || isLikelySplicedCells(line)) {
                    skipped++;
                    continue;
                }
                toTranslate.add(line);
                toTranslateIdx.add(i);
            }
            if (skipped > 0) {
                log.info("跳过 {} 行低置信度/疑似装饰噪声文字，taskId={}", skipped, task.getId());
            }

            List<String> translations = new ArrayList<>(Collections.nCopies(lines.size(), null));
            if (!toTranslate.isEmpty()) {
                List<String> partial = translateService.translate(toTranslate, apiKey, model, instruction);
                for (int i = 0; i < toTranslateIdx.size() && i < partial.size(); i++) {
                    translations.set(toTranslateIdx.get(i), partial.get(i));
                }
            }
            // 注意：这里不删 tempFile，复核确认阶段还要用它读原图做预览和最终重画。
            task.markAwaitingReview(tempFile, lines, translations);
        } catch (ServiceException e) {
            task.markError(e.getMsg());
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        } catch (Exception e) {
            log.error("图片翻译任务失败，taskId={}", task.getId(), e);
            task.markError("翻译失败，请稍后重试");
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    /** 复核阶段给前端看的数据：每行的坐标 + OCR 原文 + AI 建议译文（null 表示建议保留原图不动）。 */
    public List<PreviewLine> getPreview(String taskId) {
        TranslateTask task = getTask(taskId);
        if (task.getStatus() != TranslateTask.Status.REVIEW) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "任务当前不在待确认状态");
        }
        List<OcrLine> lines = task.getLines();
        List<String> translations = task.getTranslations();
        List<PreviewLine> result = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            OcrLine line = lines.get(i);
            result.add(new PreviewLine(i, line.getX0(), line.getY0(), line.getX1(), line.getY1(),
                    line.getText(), translations.get(i)));
        }
        return result;
    }

    /** 复核阶段展示给用户看的原图（还没擦字重画过）。 */
    public File getSourceFileForPreview(String taskId) {
        TranslateTask task = getTask(taskId);
        if (task.getStatus() != TranslateTask.Status.REVIEW || task.getSourceFile() == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "当前没有可预览的原图");
        }
        return task.getSourceFile();
    }

    /** 用户确认（可能改过部分译文）后，异步做最终擦字重画。 */
    public void confirmAndRender(String taskId, List<String> editedTranslations) {
        TranslateTask task = getTask(taskId);
        if (task.getStatus() != TranslateTask.Status.REVIEW) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "任务当前不在待确认状态");
        }
        List<OcrLine> lines = task.getLines();
        if (editedTranslations == null || editedTranslations.size() != lines.size()) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "提交的译文条数和原文行数不一致");
        }
        task.markRendering();
        workerPool.submit(() -> doRender(task, editedTranslations));
    }

    private void doRender(TranslateTask task, List<String> translations) {
        File sourceFile = task.getSourceFile();
        try {
            byte[] resultBytes = redrawService.redraw(sourceFile, task.getLines(), translations);
            File resultFile = File.createTempFile("imgtranslate-out-", ".png");
            Files.write(resultFile.toPath(), resultBytes);
            task.markDone(resultFile);
        } catch (ServiceException e) {
            task.markError(e.getMsg());
        } catch (Exception e) {
            log.error("图片重画失败，taskId={}", task.getId(), e);
            task.markError("生成失败，请稍后重试");
        } finally {
            if (sourceFile != null) {
                //noinspection ResultOfMethodCallIgnored
                sourceFile.delete();
            }
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
        if (compact.length() <= properties.getShortFragmentMaxLength()) {
            if (line.getConfidence() < properties.getShortFragmentMinConfidence()) {
                return true;
            }
            // 单独置信度过关也不够：徽标外圈弯曲装饰字有时会被 tesseract 纵向拉长识别成
            // 一两个字符、但置信度恰好够高的碎片（实测案例：字符"y"，置信度79，行高却
            // 高达106px，远超同页正常短文本的20来px）。短碎片配上异常大的行高，基本可以
            // 确定是这类伪影，一并按噪声跳过。
            if (line.height() > properties.getShortFragmentMaxHeight()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断一行是不是 tesseract 把不同表格列/单元格的文字错误拼接成了一行——
     * 密集多栏表格上 {@code --psm 11} 常见的分段错误：把本该属于不同单元格的文字
     * 归到同一个 (block, par, line)，行内会出现一段异常大的空白间隙（对应原本的列间距）。
     * 命中就跳过翻译和重画，保留原图不动，比画出跨列拼接出的错误内容更安全。
     */
    private boolean isLikelySplicedCells(OcrLine line) {
        int height = line.height();
        if (height <= 0) {
            return false;
        }
        return line.getMaxWordGap() > height * properties.getMaxWordGapToHeightRatio();
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
        deleteTaskFiles(task);
    }

    private void cleanupStaleTasks() {
        long now = System.currentTimeMillis();
        tasks.entrySet().removeIf(entry -> {
            TranslateTask task = entry.getValue();
            boolean stale = now - task.getCreatedAt() > TASK_TTL_MS;
            if (stale) {
                deleteTaskFiles(task);
            }
            return stale;
        });
    }

    /** 结果文件、以及复核阶段一直留着没删的原图临时文件，一并清理。 */
    private void deleteTaskFiles(TranslateTask task) {
        if (task == null) {
            return;
        }
        if (task.getResultFile() != null) {
            //noinspection ResultOfMethodCallIgnored
            task.getResultFile().delete();
        }
        if (task.getSourceFile() != null) {
            //noinspection ResultOfMethodCallIgnored
            task.getSourceFile().delete();
        }
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
