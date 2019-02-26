package org.ernest.applications.trampoline.config;

import org.ernest.applications.trampoline.entities.BuildToolsEnumConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new BuildToolsEnumConverter());
    }
}
