package cn.chinacici.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    /**
     * CORS 允许的前端 Origin 白名单。
     *
     * <p>注意 Origin 不包含路径，例如前端页面是
     * {@code http://110.40.132.172/suanming}，浏览器发出的 Origin 是
     * {@code http://110.40.132.172}。</p>
     */
    @Value("${suanming.cors.allowed-origin-patterns:http://110.40.132.172,http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(resolveAllowedOriginPatterns())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    private String[] resolveAllowedOriginPatterns() {
        if (!StringUtils.hasText(allowedOriginPatterns)) {
            return new String[]{"http://110.40.132.172"};
        }
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }
}
