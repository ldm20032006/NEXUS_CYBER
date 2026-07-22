package demo.server.common.response;

public record FieldViolation(
        String field,
        String message
) {
}
