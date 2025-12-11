package org.delcom.app.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Logika: 
        // Jika browser minta URL: http://localhost:8080/uploads/foto.jpg
        // Ambil file dari folder fisik: ./uploads/foto.jpg (di root project)
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}