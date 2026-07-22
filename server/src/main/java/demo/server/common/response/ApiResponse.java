package demo.server.common.response;

import demo.server.common.logging.CorrelationIdFilter;
import lombok.Builder;
import org.slf4j.MDC;

import java.time.Instant;

@Builder
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp,
        String correlationId
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .correlationId(MDC.get(CorrelationIdFilter.MDC_KEY))
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "OK");
    }
}
