package demo.server.common.logging;

import java.util.regex.Pattern;

public final class LogMasker {

    private static final Pattern EMAIL = Pattern.compile("(?i)([a-z0-9._%+-])([a-z0-9._%+-]*)(@)([a-z0-9.-]+)");
    private static final Pattern PHONE = Pattern.compile("\\b(\\+?\\d{2,3})?\\d{3,4}\\d{4}\\b");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)bearer\\s+[a-z0-9._~+/=-]+");
    private static final Pattern SENSITIVE_JSON_STRING = Pattern.compile(
            "(?i)(\"(?:password|token|secret|authorization|jwt|refreshToken|accessToken)\"\\s*:\\s*\")([^\"]+)(\")");
    private static final Pattern SENSITIVE_PAIR = Pattern.compile(
            "(?i)(password|token|secret|authorization|jwt)(\\s*[:=]\\s*)([^,\\s}\\\"]+)");

    private LogMasker() {
    }

    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = EMAIL.matcher(value).replaceAll("$1***$3$4");
        masked = PHONE.matcher(masked).replaceAll("***");
        masked = BEARER_TOKEN.matcher(masked).replaceAll("Bearer ***");
        masked = SENSITIVE_JSON_STRING.matcher(masked).replaceAll("$1***$3");
        return SENSITIVE_PAIR.matcher(masked).replaceAll("$1$2***");
    }
}
