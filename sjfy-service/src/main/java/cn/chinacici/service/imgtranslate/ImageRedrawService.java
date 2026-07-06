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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
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

        int bx0 = Math.max(0, x0 - PAD_X);
        int by0 = Math.max(0, y0 - PAD_Y);
        int bx1 = Math.min(image.getWidth(), x1 + PAD_X);
        int by1 = Math.min(image.getHeight(), y1 + PAD_Y);

        g.setColor(bg);
        g.fillRect(bx0, by0, bx1 - bx0, by1 - by0);

        if (translated.isEmpty()) {
            return;
        }

        int maxH = line.height();
        if (maxH > ANOMALY_HEIGHT_THRESHOLD && !BIG_HEADERS.contains(line.getText().trim())) {
            maxH = ANOMALY_HEIGHT_CAP;
        }
        int maxW = (int) (line.width() * 1.4 + 40);

        g.setColor(fg);
        Font font = fitFont(g, translated, maxW, maxH);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        int textHeight = fm.getAscent() + fm.getDescent();
        int drawY = y0 + (y1 - y0 - textHeight) / 2 + fm.getAscent();
        g.drawString(translated, x0, drawY);
    }

    private Font fitFont(Graphics2D g, String text, int maxW, int maxH) {
        int size = Math.max(10, (int) (maxH * 0.95));
        Font font = loadBaseFont().deriveFont((float) size);
        while (size > 8) {
            font = loadBaseFont().deriveFont((float) size);
            FontMetrics fm = g.getFontMetrics(font);
            int w = fm.stringWidth(text);
            int h = fm.getAscent() + fm.getDescent();
            if (w <= maxW && h <= maxH * 1.35) {
                return font;
            }
            size--;
        }
        return font;
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
