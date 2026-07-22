package demo.server.exception;

public class ForbiddenException extends ApplicationException {

    public ForbiddenException(String message) {
        super(message);
    }
}
