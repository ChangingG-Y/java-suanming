package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.FileRespDto;
import cn.chinacici.service.order.entity.LoFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileOrderService {
    FileRespDto upload(MultipartFile file, Integer userId);
    LoFile getFile(Integer id);
}
