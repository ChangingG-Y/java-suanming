package cn.chinacici.service.imgtranslate;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.imgtranslate.config.ImgTranslateProperties;
import cn.chinacici.service.imgtranslate.dto.OcrLine;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 抹除原文字、按原位置画上译文，尽量保持排版/配色不变。
 *
 * <p>做法：背景色取文字框四周一圈像素的中位数；文字颜色取框内与背景色差异最大的像素中位数；
 * 字号按文字框高度自适应，异常高的框（OCR 误合并导致）做上限保护，避免字号爆炸溢出重叠。</p>
 */
@Service
public class ImageRedrawService {
    private static final Set<String> BIG_HEADERS = new HashSet<>();
    private static final int ANOMALY_HEIGHT_THRESHOLD = 70;
    private static final int ANOMALY_HEIGHT_CAP = 48;
    private static final int PAD_X = 6;
    private static final int PAD_Y = 4;
    /** 两行换行方案的起始试探字号（单行缩到 9px 都放不下才会用到这个分支，见 fitLayout）。 */
    private static final int MIN_SINGLE_LINE_SIZE = 14;
    /**
     * 换成两行时，允许擦除+绘制区域比原框高出的倍数上限。人工改的译文/AI 补充翻译的译文
     * 长度不可控，原框往往只有一行字的高度，两行必然要占用更多垂直空间；这个上限是为了
     * 避免侵占相邻表格行——超过这个倍数宁可继续缩字号，也不再扩高。
     */
    private static final double MAX_HEIGHT_GROWTH_RATIO = 1.9;

    static {
        BIG_HEADERS.add("PROFESSIONAL TECHNOLOGY");
        BIG_HEADERS.add("CONTENT");
    }

    private final ImgTranslateProperties properties;
    private Font baseFont;

    public ImageRedrawService(ImgTranslateProperties properties) {
        this.properties = properties;
    }

    public byte[] redraw(File imageFile, List<OcrLine> lines, List<String> translations) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                throw new ServiceException(ResultCode.PARAMETER_ERROR, "无法读取图片，格式可能不受支持");
            }
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int n = Math.min(lines.size(), translations.size());
            for (int i = 0; i < n; i++) {
                OcrLine line = lines.get(i);
                String translated = translations.get(i);
                if (translated == null) {
                    continue;
                }
                drawOne(g, image, line, translated.trim());
            }
            g.dispose();

            // 存 PNG，不做任何有损压缩：PNG 是无损格式，画完之后每个像素原样保留。
            // JPEG 哪怕质量拉到 95%，对表格里大片纯色底纹（灰色/白色斑马纹）依然会有
            // 肉眼可见的色块、发花，这正是之前"底纹样式变了"的根源。
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "图片处理失败");
        }
    }

    private void drawOne(Graphics2D g, BufferedImage image, OcrLine line, String translated) {
        int x0 = line.getX0();
        int y0 = line.getY0();
        int x1 = line.getX1();
        int y1 = line.getY1();

        Color bg = sampleBackground(image, x0, y0, x1, y1);
        Color fg = sampleTextColor(image, x0, y0, x1, y1, bg);

        int maxH = line.height();
        if (maxH > ANOMALY_HEIGHT_THRESHOLD && !BIG_HEADERS.contains(line.getText().trim())) {
            maxH = ANOMALY_HEIGHT_CAP;
        }
        int maxW = (int) (line.width() * 1.4 + 40);

        TextLayout layout = translated.isEmpty() ? null : fitLayout(g, translated, maxW, maxH);
        // 人工改的译文/AI 补充翻译的长度不可控，一行放不下就换两行——两行需要的高度可能
        // 超过原框，擦除区域按需要的高度扩展，以原框的垂直中心为基准对称扩，不然只扩下沿
        // 会看起来文字往下坠、和原有排版对不上。
        int neededHeight = layout != null ? layout.totalHeight : (y1 - y0);
        int centerY = (y0 + y1) / 2;
        int halfHeight = Math.max((y1 - y0) / 2, neededHeight / 2);

        int bx0 = Math.max(0, x0 - PAD_X);
        int bx1 = Math.min(image.getWidth(), x1 + PAD_X);
        int by0 = Math.max(0, centerY - halfHeight - PAD_Y);
        int by1 = Math.min(image.getHeight(), centerY + halfHeight + PAD_Y);

        g.setColor(bg);
        g.fillRect(bx0, by0, bx1 - bx0, by1 - by0);

        if (translated.isEmpty() || layout == null) {
            return;
        }

        g.setColor(fg);
        g.setFont(layout.font);
        FontMetrics fm = g.getFontMetrics(layout.font);
        int startY = centerY - layout.totalHeight / 2;
        for (int i = 0; i < layout.lines.size(); i++) {
            String ln = layout.lines.get(i);
            int lineTop = startY + i * layout.lineHeight;
            int tx = x0;
            if (layout.lines.size() > 1) {
                // 换行之后每行长度不一样，居中对齐比全部靠左看起来更自然
                int lw = fm.stringWidth(ln);
                tx = x0 + Math.max(0, (line.width() - lw) / 2);
            }
            g.drawString(ln, tx, lineTop + fm.getAscent());
        }
    }

    /** 单行字体、单行/两行内容、行高：为了在换行时能一并算出擦除区域该扩多高。 */
    private static final class TextLayout {
        final Font font;
        final List<String> lines;
        final int lineHeight;
        final int totalHeight;

        TextLayout(Font font, List<String> lines, int lineHeight) {
            this.font = font;
            this.lines = lines;
            this.lineHeight = lineHeight;
            this.totalHeight = lineHeight * lines.size();
        }
    }

    private TextLayout fitLayout(Graphics2D g, String text, int maxW, int maxH) {
        // 1) 单行优先，一路缩到 9px——这是原来就验证过、绝大多数正常表格内容（尤其密集
        // 多列表格里的窄列）都走这条路径，效果一直是好的。换行只应该是这条路径彻底失败
        // 之后的兜底方案，不能因为想支持换行就提前在 14px 截断，把本来缩到 10~13px 就能
        // 放下的正常内容也挤去换行、白白扩大擦除区域侵占相邻行（上线后图4、图5验证过
        // 提前截断确实会让这类内容变差，这里改回和原版一致的下限）。
        for (int size = Math.max(10, (int) (maxH * 0.95)); size > 8; size--) {
            Font font = loadBaseFont().deriveFont((float) size);
            FontMetrics fm = g.getFontMetrics(font);
            int w = fm.stringWidth(text);
            int h = fm.getAscent() + fm.getDescent();
            if (w <= maxW && h <= maxH * 1.35) {
                return new TextLayout(font, Collections.singletonList(text), h);
            }
        }

        // 2) 单行缩到 9px 都放不下，说明文字长度和框已经明显不匹配（通常是人工改的
        // 译文/AI 补充翻译偏长），这时候才值得尝试换成两行，允许擦除/绘制区域适度增高
        int maxHFor2Lines = (int) (maxH * MAX_HEIGHT_GROWTH_RATIO);
        for (int size = Math.max(MIN_SINGLE_LINE_SIZE, (int) (maxH * 0.85)); size >= 10; size--) {
            Font font = loadBaseFont().deriveFont((float) size);
            FontMetrics fm = g.getFontMetrics(font);
            String[] wrapped = wrapToTwoLines(fm, text);
            if (wrapped.length < 2) {
                continue; // 分不出两行（比如就一两个字），走兜底单行方案
            }
            int lineH = fm.getAscent() + fm.getDescent();
            int totalH = lineH * 2;
            if (Math.max(fm.stringWidth(wrapped[0]), fm.stringWidth(wrapped[1])) <= maxW && totalH <= maxHFor2Lines) {
                return new TextLayout(font, java.util.Arrays.asList(wrapped), lineH);
            }
        }

        // 3) 兜底：跟之前一样，单行一路缩到 8px，能塞多少塞多少，不再讲究好看
        int size = Math.max(10, (int) (maxH * 0.95));
        Font font = loadBaseFont().deriveFont((float) size);
        while (size > 8) {
            font = loadBaseFont().deriveFont((float) size);
            FontMetrics fm = g.getFontMetrics(font);
            int w = fm.stringWidth(text);
            int h = fm.getAscent() + fm.getDescent();
            if (w <= maxW && h <= maxH * 1.35) {
                break;
            }
            size--;
        }
        FontMetrics fm = g.getFontMetrics(font);
        return new TextLayout(font, Collections.singletonList(text), fm.getAscent() + fm.getDescent());
    }

    /**
     * 把一段文字从中间附近断成两行，尽量让两行宽度接近。英文短语优先在空格处断
     * （不然会把单词从中间切开，很难看）；中文没有天然词边界，就按字符宽度找
     * 让两行差距最小的断点。
     */
    private String[] wrapToTwoLines(FontMetrics fm, String text) {
        int n = text.length();
        if (n < 2) {
            return new String[]{text};
        }
        int mid = n / 2;
        int radius = Math.max(1, n / 3);
        int lo = Math.max(1, mid - radius);
        int hi = Math.min(n - 1, mid + radius);

        int spaceSplit = -1;
        int spaceSplitDist = Integer.MAX_VALUE;
        for (int i = lo; i <= hi; i++) {
            if (text.charAt(i) == ' ') {
                int dist = Math.abs(i - mid);
                if (dist < spaceSplitDist) {
                    spaceSplitDist = dist;
                    spaceSplit = i;
                }
            }
        }

        int split;
        if (spaceSplit >= 0) {
            split = spaceSplit;
        } else {
            int bestSplit = mid;
            int bestDiff = Integer.MAX_VALUE;
            for (int i = lo; i <= hi; i++) {
                int diff = Math.abs(fm.stringWidth(text.substring(0, i)) - fm.stringWidth(text.substring(i)));
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestSplit = i;
                }
            }
            split = bestSplit;
        }

        String a = text.substring(0, split).trim();
        String b = text.substring(split).trim();
        if (a.isEmpty() || b.isEmpty()) {
            return new String[]{text};
        }
        return new String[]{a, b};
    }

    private synchronized Font loadBaseFont() {
        if (baseFont != null) {
            return baseFont;
        }
        try {
            File fontFile = new File(properties.getFontPath());
            if (fontFile.exists()) {
                baseFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            } else {
                // 兜底：classpath 资源
                baseFont = Font.createFont(Font.TRUETYPE_FONT,
                        getClass().getClassLoader().getResourceAsStream(properties.getFontPath()));
            }
        } catch (Exception e) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR,
                    "找不到中文字体文件（" + properties.getFontPath() + "），请检查部署配置");
        }
        return baseFont;
    }

    private Color sampleBackground(BufferedImage image, int x0, int y0, int x1, int y1) {
        int pad = 6;
        int w = image.getWidth();
        int h = image.getHeight();
        java.util.List<int[]> samples = new java.util.ArrayList<>();

        collectStrip(image, samples, Math.max(0, x0 - pad), Math.max(0, y0 - pad * 2), Math.min(w, x1 + pad), Math.max(0, y0 - pad));
        collectStrip(image, samples, Math.max(0, x0 - pad), Math.min(h, y1 + pad), Math.min(w, x1 + pad), Math.min(h, y1 + pad * 2));
        collectStrip(image, samples, Math.max(0, x0 - pad * 2), Math.max(0, y0 - pad), Math.max(0, x0 - pad), Math.min(h, y1 + pad));
        collectStrip(image, samples, Math.min(w, x1 + pad), Math.max(0, y0 - pad), Math.min(w, x1 + pad * 2), Math.min(h, y1 + pad));

        if (samples.isEmpty()) {
            collectStrip(image, samples, x0, y0, x1, y1);
        }
        return medianColor(samples, new Color(255, 255, 255));
    }

    private Color sampleTextColor(BufferedImage image, int x0, int y0, int x1, int y1, Color bg) {
        java.util.List<int[]> samples = new java.util.ArrayList<>();
        java.util.List<int[]> distinct = new java.util.ArrayList<>();
        int w = image.getWidth();
        int h = image.getHeight();
        int xa = Math.max(0, x0);
        int ya = Math.max(0, y0);
        int xb = Math.min(w, x1);
        int yb = Math.min(h, y1);
        for (int yy = ya; yy < yb; yy++) {
            for (int xx = xa; xx < xb; xx++) {
                int rgb = image.getRGB(xx, yy);
                int[] px = {(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
                samples.add(px);
                int dist = Math.abs(px[0] - bg.getRed()) + Math.abs(px[1] - bg.getGreen()) + Math.abs(px[2] - bg.getBlue());
                if (dist > 60) {
                    distinct.add(px);
                }
            }
        }
        double bgLum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        Color fallback = bgLum < 128 ? new Color(255, 255, 255) : new Color(20, 20, 20);
        if (distinct.size() < Math.max(5, samples.size() * 0.02)) {
            return fallback;
        }
        return medianColor(distinct, fallback);
    }

    private void collectStrip(BufferedImage image, java.util.List<int[]> samples, int xa, int ya, int xb, int yb) {
        if (xb <= xa || yb <= ya) {
            return;
        }
        for (int y = ya; y < yb; y++) {
            for (int x = xa; x < xb; x++) {
                int rgb = image.getRGB(x, y);
                samples.add(new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF});
            }
        }
    }

    private Color medianColor(java.util.List<int[]> samples, Color fallback) {
        if (samples.isEmpty()) {
            return fallback;
        }
        int n = samples.size();
        int[] rs = new int[n];
        int[] gs = new int[n];
        int[] bs = new int[n];
        for (int i = 0; i < n; i++) {
            rs[i] = samples.get(i)[0];
            gs[i] = samples.get(i)[1];
            bs[i] = samples.get(i)[2];
        }
        java.util.Arrays.sort(rs);
        java.util.Arrays.sort(gs);
        java.util.Arrays.sort(bs);
        return new Color(rs[n / 2], gs[n / 2], bs[n / 2]);
    }
}
