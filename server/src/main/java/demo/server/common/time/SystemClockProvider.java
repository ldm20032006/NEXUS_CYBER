package demo.server.common.time;

import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class SystemClockProvider implements ClockProvider {

    private static final Clock UTC_CLOCK = Clock.systemUTC();

    @Override
    public Clock clock() {
        return UTC_CLOCK;
    }
}
