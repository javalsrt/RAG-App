package com.znxsgl.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件的本地路径到 /uploads/ URL，使用标准 file URI 避免 Windows 路径问题
        String uploadPath = Paths.get(System.getProperty("user.dir"), "uploads").toUri().toString();
        log.info("配置静态资源映射: /uploads/** -> {}", uploadPath);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
