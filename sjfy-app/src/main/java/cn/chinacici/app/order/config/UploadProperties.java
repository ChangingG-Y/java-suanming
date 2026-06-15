package cn.chinacici.app.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 文件上传根目录配置，读取 love.upload.base-dir */
@Component
@ConfigurationProperties(prefix = "love.upload")
public class UploadProperties {
    private String baseDir = "./uploads";

    public String getBaseDir() { return baseDir; }
    public void setBaseDir(String baseDir) { this.baseDir = baseDir; }
}
