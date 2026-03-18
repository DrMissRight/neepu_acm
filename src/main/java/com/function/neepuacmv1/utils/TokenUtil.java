package com.function.neepuacmv1.utils;

import com.function.neepuacmv1.constant.RoleCodes;

import java.util.UUID;

/**
 * Token 工具：
 * 这里使用 UUID 作为 Token（更贴近“Redis 会话”模式，避免引入 JWT 依赖）。
 * 如果你项目已引入 JWT，可替换为 JWT 生成/解析，但 Redis 仍作为最终会话依据。
 */
public final class TokenUtil {
    private TokenUtil() {}

    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 默认新注册用户角色：GUEST/TRAINEE 可按业务选择，这里用 TRAINEE 更贴合集训队 */
    public static String defaultRegisterRole() {
        return RoleCodes.TRAINEE;
    }
}
