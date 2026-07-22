package demo.server.exception;

public class ConcurrencyConflictException extends ApplicationException {

    public ConcurrencyConflictException(String message) {
        super(message);
    }
}
