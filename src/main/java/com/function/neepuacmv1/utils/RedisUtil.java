package com.function.neepuacmv1.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Redis 操作工具类（String 维度，简单够用） */
@Component
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public Object get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public boolean delete(String key) {
        Boolean b = stringRedisTemplate.delete(key);
        return b != null && b;
    }

    public long incr(String key, long ttlSeconds) {
        Long v = stringRedisTemplate.opsForValue().increment(key);
        if (v != null && v == 1L) {
            stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return v == null ? 0 : v;
    }

    // ===== 新增：Set 操作 =====
    public void sadd(String key, String member) {
        stringRedisTemplate.opsForSet().add(key, member);
    }

    public void srem(String key, String member) {
        stringRedisTemplate.opsForSet().remove(key, member);
    }

    public Set<String> smembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    public void expire(String key, long ttlSeconds) {
        stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
    }
}
