package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.FileRespDto;
import cn.chinacici.service.order.entity.LoFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileOrderService {
    /** mode: "dish" = 缩略图only (800x800正方形); "review" = 原图+缩略图(长边不超1500) */
    FileRespDto upload(MultipartFile file, Integer userId, String mode);
    LoFile getFile(Integer id);
}
