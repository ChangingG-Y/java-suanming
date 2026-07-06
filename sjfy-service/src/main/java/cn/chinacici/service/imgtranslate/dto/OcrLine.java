package cn.chinacici.service.imgtranslate.dto;

/** 一行 OCR 文字及其像素坐标（同一 block/par/line 内的单词已合并）。 */
public class OcrLine {
    private int x0;
    private int y0;
    private int x1;
    private int y1;
    private String text;
    /** tesseract 识别置信度（0~100），取行内所有单词置信度的平均值。 */
    private double confidence;
    /**
     * 行内相邻两个单词之间最大的水平间隙（像素）。正常同一格/同一句内的词间距很小，
     * 如果某处间距异常大，大概率是 tesseract 把不同表格列/单元格的文字错误合并成了一行
     * （--psm 11 在密集多栏表格上常见的分段错误），跨越了原本的空白列间距。
     */
    private int maxWordGap;

    public OcrLine(int x0, int y0, int x1, int y1, String text, double confidence, int maxWordGap) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.text = text;
        this.confidence = confidence;
        this.maxWordGap = maxWordGap;
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

    public double getConfidence() {
        return confidence;
    }

    public int getMaxWordGap() {
        return maxWordGap;
    }
}
