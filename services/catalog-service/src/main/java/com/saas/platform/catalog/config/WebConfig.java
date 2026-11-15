package com.saas.platform.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Sirve los archivos subidos a /uploads/** desde el directorio local
 * configurado en app.uploads.dir. Sin auth (publicos).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir:./uploads}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(uploadsDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolute + "/")
                .setCachePeriod(3600);
    }
}
