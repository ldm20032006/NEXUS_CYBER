package demo.server.common.audit;

import java.util.Optional;

public final class AuditRequestContextHolder {

    private static final ThreadLocal<AuditRequestContext> CONTEXT = new ThreadLocal<>();

    private AuditRequestContextHolder() {
    }

    public static void set(AuditRequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<AuditRequestContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
