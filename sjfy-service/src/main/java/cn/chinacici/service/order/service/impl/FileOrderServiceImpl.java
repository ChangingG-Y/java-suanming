package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.FileRespDto;
import cn.chinacici.service.order.entity.LoFile;
import cn.chinacici.service.order.mapper.LoFileMapper;
import cn.chinacici.service.order.service.FileOrderService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileOrderServiceImpl implements FileOrderService {
    private final LoFileMapper fileMapper;
    private final String baseDir;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    public FileOrderServiceImpl(LoFileMapper fileMapper,
                                @Value("${love.upload.base-dir:./uploads}") String baseDir) {
        this.fileMapper = fileMapper;
        this.baseDir = baseDir;
    }

    @Override
    public FileRespDto upload(MultipartFile file, Integer userId, String mode) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请选择文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "只支持图片文件(JPG/PNG/GIF/WebP)");
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ext = getExt(file.getOriginalFilename(), contentType);

        File baseDirFile = new File(baseDir).getAbsoluteFile();
        String relFilePath;
        String relThumbPath = "thumbnails/" + dateDir + "/" + uuid + "_thumb.jpg";
        File destThumb = new File(baseDirFile, relThumbPath);
        destThumb.getParentFile().mkdirs();

        try {
            if ("dish".equals(mode)) {
                // 菜品：前端已裁好 800x800，直接存为缩略图，不保留原图
                file.transferTo(destThumb);
                relFilePath = relThumbPath; // 原图路径指向同一文件
            } else {
                // 评价（默认）：保留原图，缩略图长边不超 1500
                relFilePath = "files/" + dateDir + "/" + uuid + "." + ext;
                File destFile = new File(baseDirFile, relFilePath);
                destFile.getParentFile().mkdirs();
                file.transferTo(destFile);
                Thumbnails.of(destFile)
                    .size(1500, 1500)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .toFile(destThumb);
            }
        } catch (IOException e) {
            throw new ServiceException(ResultCode.UNKNOWN_ERROR, "文件保存失败: " + e.getMessage());
        }

        int now = (int)(System.currentTimeMillis() / 1000);
        LoFile loFile = new LoFile();
        loFile.setOriginalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : uuid + "." + ext);
        loFile.setFilePath(relFilePath);
        loFile.setThumbnailPath(relThumbPath);
        loFile.setFileSize(file.getSize());
        loFile.setContentType(contentType);
        loFile.setUploadBy(userId);
        loFile.setIsDeleted(0);
        loFile.setCreatedAt(now);
        loFile.setUpdatedAt(now);
        fileMapper.insert(loFile);

        FileRespDto dto = new FileRespDto();
        dto.setId(loFile.getId());
        dto.setUrl("/api/order/file/" + loFile.getId());
        dto.setThumbnailUrl("/api/order/file/" + loFile.getId() + "/thumbnail");
        dto.setOriginalName(loFile.getOriginalName());
        dto.setFileSize(loFile.getFileSize());
        return dto;
    }

    @Override
    public LoFile getFile(Integer id) {
        return fileMapper.selectById(id);
    }

    private String getExt(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        switch (contentType.toLowerCase()) {
            case "image/jpeg": case "image/jpg": return "jpg";
            case "image/png": return "png";
            case "image/gif": return "gif";
            case "image/webp": return "webp";
            default: return "jpg";
        }
    }
}
