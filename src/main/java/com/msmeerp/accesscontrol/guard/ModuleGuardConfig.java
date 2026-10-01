package com.msmeerp.accesscontrol.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ModuleGuardConfig implements WebMvcConfigurer {

    private final ModuleGuardInterceptor moduleGuardInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moduleGuardInterceptor);
    }
}
