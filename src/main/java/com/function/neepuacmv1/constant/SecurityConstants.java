package com.function.neepuacmv1.constant;

/** 安全相关常量 */
public final class SecurityConstants {
    private SecurityConstants() {}

    /** Header 优先级：Authorization Bearer <token>，其次 X-Token */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_X_TOKEN = "X-Token";
    public static final String BEARER_PREFIX = "Bearer ";

    /** Redis TTL */
    public static final long TOKEN_TTL_SECONDS = 7 * 24 * 3600; // 7 天
    public static final long USER_CACHE_TTL_SECONDS = 30 * 60;  // 30 分钟
    public static final long ROLE_CACHE_TTL_SECONDS = 30 * 60;  // 30 分钟
    public static final long CAPTCHA_TTL_SECONDS = 5 * 60;      // 5 分钟
    public static final long VERIFY_CODE_TTL_SECONDS = 5 * 60;  // 5 分钟
    public static final long LOGIN_FAIL_TTL_SECONDS = 10 * 60;  // 10 分钟

    public static final int MAX_LOGIN_FAIL = 5;
}
