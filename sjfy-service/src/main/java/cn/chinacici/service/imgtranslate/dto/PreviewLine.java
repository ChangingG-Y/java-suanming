package cn.chinacici.service.imgtranslate.dto;

/** 复核阶段展示给前端的一行：原文坐标 + OCR 原文 + AI 建议译文（null 表示建议保留原图不动）。 */
public class PreviewLine {
    private int index;
    private int x0;
    private int y0;
    private int x1;
    private int y1;
    private String text;
    private String translated;

    public PreviewLine(int index, int x0, int y0, int x1, int y1, String text, String translated) {
        this.index = index;
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.text = text;
        this.translated = translated;
    }

    public int getIndex() {
        return index;
    }

    public int getX0() {
        return x0;
    }

    public int getY0() {
        return y0;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public String getText() {
        return text;
    }

    public String getTranslated() {
        return translated;
    }
}
