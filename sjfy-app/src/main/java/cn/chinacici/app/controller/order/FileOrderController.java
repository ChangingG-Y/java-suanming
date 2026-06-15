package cn.chinacici.app.controller.order;

import cn.chinacici.app.order.config.UploadProperties;
import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.FileRespDto;
import cn.chinacici.service.order.entity.LoFile;
import cn.chinacici.service.order.service.FileOrderService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/order/file")
public class FileOrderController {
    private final FileOrderService fileOrderService;
    private final UserOrderService userOrderService;
    private final UploadProperties uploadProperties;

    public FileOrderController(FileOrderService fileOrderService,
                               UserOrderService userOrderService,
                               UploadProperties uploadProperties) {
        this.fileOrderService = fileOrderService;
        this.userOrderService = userOrderService;
        this.uploadProperties = uploadProperties;
    }

    /** POST /api/order/file/upload - 需要登录 */
    @PostMapping("/upload")
    public ResponseData<FileRespDto> upload(
        @RequestParam("file") MultipartFile file,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        return ResponseData.success(fileOrderService.upload(file, userId));
    }

    /** GET /api/order/file/{id} - 公开访问 */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Integer id) throws IOException {
        return serveFile(id, false);
    }

    /** GET /api/order/file/{id}/thumbnail - 公开访问 */
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> downloadThumbnail(@PathVariable Integer id) throws IOException {
        return serveFile(id, true);
    }

    private ResponseEntity<byte[]> serveFile(Integer id, boolean thumbnail) throws IOException {
        LoFile loFile = fileOrderService.getFile(id);
        if (loFile == null) return ResponseEntity.notFound().build();
        String relativePath = thumbnail ? loFile.getThumbnailPath() : loFile.getFilePath();
        if (relativePath == null || relativePath.isEmpty()) return ResponseEntity.notFound().build();
        File f = new File(uploadProperties.getBaseDir(), relativePath);
        if (!f.exists()) return ResponseEntity.notFound().build();
        byte[] bytes = Files.readAllBytes(f.toPath());
        MediaType mediaType = determineMediaType(loFile.getContentType(), thumbnail);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
            .body(bytes);
    }

    private MediaType determineMediaType(String contentType, boolean thumbnail) {
        if (thumbnail) return MediaType.IMAGE_JPEG;
        if (contentType != null) {
            try { return MediaType.parseMediaType(contentType); } catch (Exception ignored) {}
        }
        return MediaType.IMAGE_JPEG;
    }
}
