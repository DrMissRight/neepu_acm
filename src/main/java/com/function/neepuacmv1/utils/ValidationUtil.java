package com.function.neepuacmv1.utils;

import java.util.regex.Pattern;

/** 参数校验工具 */
public final class ValidationUtil {
    private ValidationUtil() {}

    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{7,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean validUsername(String s) {
        return s != null && USERNAME.matcher(s).matches();
    }

    public static boolean validPhone(String s) {
        return s != null && PHONE.matcher(s).matches();
    }

    public static boolean validEmail(String s) {
        return s != null && EMAIL.matcher(s).matches();
    }

    public static boolean validPassword(String s) {
        // 至少 8 位，包含字母与数字（你可加强为大小写+特殊字符）
        if (s == null || s.length() < 8 || s.length() > 64) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }
}
