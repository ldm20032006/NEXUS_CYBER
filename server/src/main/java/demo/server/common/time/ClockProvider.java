package demo.server.common.time;

import java.time.Clock;
import java.time.Instant;

public interface ClockProvider {

    Clock clock();

    default Instant now() {
        return Instant.now(clock());
    }
}
