package demo.server.service.notification.delivery;

import demo.server.entity.notification.Notification;
import demo.server.entity.notification.PushSubscription;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexus.notification.push-provider", havingValue = "mock", matchIfMissing = true)
public class MockBrowserPushSender implements BrowserPushSender {

    @Override
    public DeliverySendResult send(Notification notification, PushSubscription subscription) {
        if (subscription.getEndpoint().contains("fail")) {
            return DeliverySendResult.failed("Mock push delivery failed");
        }
        return DeliverySendResult.sent("Mock push delivery sent");
    }
}
