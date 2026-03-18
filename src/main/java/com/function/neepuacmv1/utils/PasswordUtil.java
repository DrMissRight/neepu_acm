package com.function.neepuacmv1.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt 工具封装 */
@Component
public class PasswordUtil {
    private final BCryptPasswordEncoder encoder;

    public PasswordUtil(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public boolean matches(String raw, String hashed) {
        return encoder.matches(raw, hashed);
    }
}
