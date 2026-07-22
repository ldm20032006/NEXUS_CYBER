package demo.server.exception;

public class RateLimitExceededException extends ApplicationException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
