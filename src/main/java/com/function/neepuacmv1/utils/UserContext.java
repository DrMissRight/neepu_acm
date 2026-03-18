package com.function.neepuacmv1.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collections;
import java.util.List;

public final class UserContext {
    private UserContext() {}

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_ROLES = "roles";

    public static Long getUserId() {
        RequestAttributes a = RequestContextHolder.getRequestAttributes();
        if (a == null) return null;
        Object v = a.getAttribute(ATTR_USER_ID, RequestAttributes.SCOPE_REQUEST);
        if (v == null) return null;
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRoles() {
        RequestAttributes a = RequestContextHolder.getRequestAttributes();
        if (a == null) return Collections.emptyList();
        Object v = a.getAttribute(ATTR_ROLES, RequestAttributes.SCOPE_REQUEST);
        if (v instanceof List) return (List<String>) v;
        return Collections.emptyList();
    }

    public static boolean isAdminLike() {
        List<String> roles = getRoles();
        return roles.contains("ADMIN") || roles.contains("COACH");
    }
}
