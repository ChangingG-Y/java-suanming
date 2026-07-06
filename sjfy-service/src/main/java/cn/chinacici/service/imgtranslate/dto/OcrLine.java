package cn.chinacici.service.imgtranslate.dto;

/** 一行 OCR 文字及其像素坐标（同一 block/par/line 内的单词已合并）。 */
public class OcrLine {
    private int x0;
    private int y0;
    private int x1;
    private int y1;
    private String text;

    public OcrLine(int x0, int y0, int x1, int y1, String text) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.text = text;
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

    public int height() {
        return y1 - y0;
    }

    public int width() {
        return x1 - x0;
    }

    public String getText() {
        return text;
    }
}
