package com.function.neepuacmv1.config;

import org.springframework.context.annotation.Configuration;

/**
 * Redis 基础配置：
 * 若你已在 application.yml 配好 spring.data.redis.*，这里通常不必额外写 Bean。
 * 保留该类用于后续序列化策略扩展。
 */
@Configuration
public class RedisConfig {}
