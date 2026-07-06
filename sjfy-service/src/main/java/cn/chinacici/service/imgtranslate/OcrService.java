package cn.chinacici.service.imgtranslate;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.imgtranslate.config.ImgTranslateProperties;
import cn.chinacici.service.imgtranslate.dto.OcrLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 调用系统 tesseract 命令行做文字识别，按 (block, par, line) 把单词级别的框合并成整行。
 *
 * <p>部署要求：服务器需要安装 tesseract-ocr 以及对应语言包（含中文时装 tesseract-ocr-chi-sim），
 * Ubuntu 上：{@code apt-get install -y tesseract-ocr tesseract-ocr-chi-sim}。</p>
 */
@Service
public class OcrService {
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final ImgTranslateProperties properties;

    public OcrService(ImgTranslateProperties properties) {
        this.properties = properties;
    }

    public List<OcrLine> extractLines(File imageFile) {
        File tsvBase = null;
        try {
            tsvBase = File.createTempFile("ocr-", "");
            String outputBase = tsvBase.getAbsolutePath();
            // tesseract 会在 outputBase 后追加 .tsv
            List<String> command = new ArrayList<>();
            command.add(properties.getTesseractPath());
            command.add(imageFile.getAbsolutePath());
            command.add(outputBase);
            command.add("--psm");
            command.add("11");
            if (properties.getOcrLang() != null && !properties.getOcrLang().isEmpty()) {
                command.add("-l");
                command.add(properties.getOcrLang());
            }
            command.add("tsv");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder procOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    procOutput.append(line).append('\n');
                }
            }

            boolean finished = process.waitFor(properties.getOcrTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ServiceException(ResultCode.UNKNOWN_ERROR, "OCR 识别超时");
            }
            if (process.exitValue() != 0) {
                log.warn("tesseract 执行失败，输出：{}", procOutput);
                throw new ServiceException(ResultCode.UNKNOWN_ERROR, "OCR 识别失败，请检查服务器是否已安装 tesseract-ocr");
            }

            File tsvFile = new File(outputBase + ".tsv");
            if (!tsvFile.exists()) {
                throw new ServiceException(ResultCode.UNKNOWN_ERROR, "OCR 未生成识别结果");
            }
            List<String> tsvLines = Files.readAllLines(tsvFile.toPath(), StandardCharsets.UTF_8);
            List<OcrLine> result = parseTsv(tsvLines);
            Files.deleteIfExists(tsvFile.toPath());
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("OCR 调用异常", e);
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "OCR 服务暂时不可用");
        } finally {
            if (tsvBase != null) {
                //noinspection ResultOfMethodCallIgnored
                tsvBase.delete();
            }
        }
    }

    /**
     * tesseract --tsv 输出列：level,page_num,block_num,par_num,line_num,word_num,left,top,width,height,conf,text
     */
    private List<OcrLine> parseTsv(List<String> tsvLines) {
        if (tsvLines.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, int[]> boxByKey = new LinkedHashMap<>();
        Map<String, StringBuilder> textByKey = new LinkedHashMap<>();

        for (int i = 1; i < tsvLines.size(); i++) {
            String row = tsvLines.get(i);
            if (row == null || row.trim().isEmpty()) {
                continue;
            }
            String[] cols = row.split("\t", -1);
            if (cols.length < 12) {
                continue;
            }
            String text = cols[11].trim();
            if (text.isEmpty()) {
                continue;
            }
            try {
                String blockNum = cols[2];
                String parNum = cols[3];
                String lineNum = cols[4];
                int left = Integer.parseInt(cols[6].trim());
                int top = Integer.parseInt(cols[7].trim());
                int width = Integer.parseInt(cols[8].trim());
                int height = Integer.parseInt(cols[9].trim());

                String key = blockNum + ":" + parNum + ":" + lineNum;
                int[] box = boxByKey.get(key);
                if (box == null) {
                    box = new int[]{left, top, left + width, top + height};
                    boxByKey.put(key, box);
                    textByKey.put(key, new StringBuilder(text));
                } else {
                    box[0] = Math.min(box[0], left);
                    box[1] = Math.min(box[1], top);
                    box[2] = Math.max(box[2], left + width);
                    box[3] = Math.max(box[3], top + height);
                    textByKey.get(key).append(' ').append(text);
                }
            } catch (NumberFormatException e) {
                // 跳过解析异常的行
            }
        }

        List<OcrLine> lines = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : boxByKey.entrySet()) {
            int[] box = entry.getValue();
            String text = textByKey.get(entry.getKey()).toString();
            lines.add(new OcrLine(box[0], box[1], box[2], box[3], text));
        }
        lines.sort((a, b) -> {
            if (a.getY0() != b.getY0()) {
                return Integer.compare(a.getY0(), b.getY0());
            }
            return Integer.compare(a.getX0(), b.getX0());
        });
        return lines;
    }
}
