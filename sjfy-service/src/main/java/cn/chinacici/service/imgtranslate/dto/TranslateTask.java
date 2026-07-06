package cn.chinacici.service.imgtranslate.dto;

import java.io.File;
import java.util.List;

/**
 * 一次图片翻译任务的状态，存内存里。
 *
 * <p>流程分两段：OCR+翻译跑完先进 {@code REVIEW} 状态，把 OCR 原文、坐标、
 * 建议译文都存在任务里，交给前端给用户看一眼、改一改，用户确认（或什么都不改直接确认）
 * 后才真正擦字重画，转 {@code DONE}。原图（{@code sourceFile}）要留到重画完才删——
 * 复核阶段用它来给用户看预览。</p>
 */
public class TranslateTask {
    public enum Status {
        PROCESSING, REVIEW, DONE, ERROR
    }

    private final String id;
    private volatile Status status = Status.PROCESSING;
    private volatile File sourceFile;
    private volatile List<OcrLine> lines;
    private volatile List<String> translations;
    private volatile File resultFile;
    private volatile String errorMsg;
    private final long createdAt = System.currentTimeMillis();

    public TranslateTask(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    /** OCR+翻译跑完，等用户在前端复核确认。 */
    public void markAwaitingReview(File sourceFile, List<OcrLine> lines, List<String> translations) {
        this.sourceFile = sourceFile;
        this.lines = lines;
        this.translations = translations;
        this.status = Status.REVIEW;
    }

    /** 用户点了确认，重新进入处理中（后台做擦字重画）。 */
    public void markRendering() {
        this.status = Status.PROCESSING;
    }

    public void markDone(File resultFile) {
        this.resultFile = resultFile;
        this.status = Status.DONE;
    }

    public void markError(String errorMsg) {
        this.errorMsg = errorMsg;
        this.status = Status.ERROR;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public List<OcrLine> getLines() {
        return lines;
    }

    public List<String> getTranslations() {
        return translations;
    }

    public File getResultFile() {
        return resultFile;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
