package com.function.neepuacmv1.security;

import java.util.List;

/** ThreadLocal 保存当前请求用户上下文 */
public final class UserContext {
    private UserContext() {}

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> ROLES = new ThreadLocal<>();

    public static void set(Long userId, List<String> roles) {
        USER_ID.set(userId);
        ROLES.set(roles);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static List<String> getRoles() {
        return ROLES.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLES.remove();
    }
}
