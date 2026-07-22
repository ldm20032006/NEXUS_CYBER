package demo.server.common.resilience;

public interface LockHandle extends AutoCloseable {

    String key();

    @Override
    void close();
}
