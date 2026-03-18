package com.function.neepuacmv1.config;



import com.function.neepuacmv1.security.AuthInterceptor;
import com.function.neepuacmv1.utils.RedisUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册拦截器 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RedisUtil redisUtil;

    public WebMvcConfig(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(redisUtil))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",   // 认证相关全部放行
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api/auth/**",
                        "/api/problems/**",
                        "/api/tags/**"
                );
    }
}
