package com.envision.epc.infrastructure.web;

import com.envision.epc.infrastructure.crypto.param.DecryptFormatterFactory;
import com.envision.epc.infrastructure.i18n.I18nConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * @author jingjing.dong
 * @since 2021/3/22-20:19
 */
@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebMvcConfig implements WebMvcConfigurer {
    private final I18nConfig config;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //设定Locale拦截器并定义拦截的URL
        registry.addInterceptor(config.localeChangeInterceptor()).addPathPatterns("/user/locale");
    }

    /**
     * 自定义静态文件请求路径和静态文件位置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    /**
     * SpringMVC全局跨域配置，SpringSecurity跨域还需要配置http.cors();
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .maxAge(3600);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
       registry.addFormatterForFieldAnnotation(new DecryptFormatterFactory());
        //registry.addConverter(new DecryptConverter());
    }
    /**
     * 为Spring Validation添加国际化支持，可以使用{}的方式自定义国际化语言
     */
    @Override
    public Validator getValidator() {
        return config.getValidator();
    }
}
