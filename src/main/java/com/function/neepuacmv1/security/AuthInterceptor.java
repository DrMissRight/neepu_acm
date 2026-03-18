package com.function.neepuacmv1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.neepuacmv1.constant.RedisKeys;
import com.function.neepuacmv1.constant.SecurityConstants;
import com.function.neepuacmv1.utils.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Token 鉴权拦截器：
 * 1) 从 Header 取 token
 * 2) Redis 校验 token -> userId
 * 3) Redis 取 roles（无则可由 Service 写入，或这里兜底查库；此处只做缓存读取）
 * 4) 若命中 @RequireRoles 注解，校验角色
 */
public class AuthInterceptor implements HandlerInterceptor {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 非方法处理器直接放行
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        // 若无 RequireRoles 且是公开接口，也可能需要放行：这里交给 WebMvcConfig 的 excludePathPatterns
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            // 允许公开接口通过（由路径排除实现）；否则这里返回 401
            RequireRoles rr0 = hm.getMethodAnnotation(RequireRoles.class);
            RequireRoles rr1 = hm.getBeanType().getAnnotation(RequireRoles.class);
            if (rr0 == null && rr1 == null) {
                return true;
            }
            writeJson(response, 401, "{\"success\":false,\"errorMsg\":\"未登录或Token缺失\",\"data\":null,\"total\":null}");
            return false;
        }

        Object userIdObj = redisUtil.get(RedisKeys.TOKEN + token);
        if (userIdObj == null) {
            writeJson(response, 401, "{\"success\":false,\"errorMsg\":\"登录已过期，请重新登录\",\"data\":null,\"total\":null}");
            return false;
        }

        Long userId = Long.valueOf(String.valueOf(userIdObj));

        // roles 读取（存储为 JSON 数组）
        List<String> roles = new ArrayList<>();
        Object rolesObj = redisUtil.get(RedisKeys.USER_ROLES + userId);
        if (rolesObj != null) {
            roles = objectMapper.readValue(String.valueOf(rolesObj), List.class);
        }

        UserContext.set(userId, roles);

        // 角色校验：方法注解优先，其次类注解
        RequireRoles rr = hm.getMethodAnnotation(RequireRoles.class);
        if (rr == null) rr = hm.getBeanType().getAnnotation(RequireRoles.class);

        if (rr != null) {
            List<String> need = Arrays.asList(rr.value());
            boolean ok = false;
            for (String r : roles) {
                if (need.contains(r)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                writeJson(response, 403, "{\"success\":false,\"errorMsg\":\"权限不足\",\"data\":null,\"total\":null}");
                return false;
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String auth = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        if (auth != null && auth.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return auth.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        }
        String x = request.getHeader(SecurityConstants.HEADER_X_TOKEN);
        return (x == null || x.isBlank()) ? null : x.trim();
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }
}
