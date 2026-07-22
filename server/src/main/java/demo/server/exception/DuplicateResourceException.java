package demo.server.exception;

public class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
