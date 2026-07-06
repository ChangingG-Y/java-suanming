package cn.chinacici.service.imgtranslate.dto;

import java.io.File;

/** 一次图片翻译任务的状态，存内存里，翻译完成后前端下载完会主动删掉临时文件。 */
public class TranslateTask {
    public enum Status {
        PROCESSING, DONE, ERROR
    }

    private final String id;
    private volatile Status status = Status.PROCESSING;
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

    public void markDone(File resultFile) {
        this.resultFile = resultFile;
        this.status = Status.DONE;
    }

    public void markError(String errorMsg) {
        this.errorMsg = errorMsg;
        this.status = Status.ERROR;
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
