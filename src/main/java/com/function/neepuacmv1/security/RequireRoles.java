package com.function.neepuacmv1.security;

import java.lang.annotation.*;

/** 方法级角色校验注解（AOP 在拦截器里做也可，这里由拦截器读取 HandlerMethod 注解） */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRoles {
    /** 允许的角色编码，满足其一即可 */
    String[] value();
}
