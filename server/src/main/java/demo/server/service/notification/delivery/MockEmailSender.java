package demo.server.service.notification.delivery;

import demo.server.entity.notification.Notification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexus.notification.email-provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailSender implements EmailSender {

    @Override
    public DeliverySendResult sendAccountSecurityEmail(String recipientEmail, Notification notification) {
        return DeliverySendResult.sent("Mock email delivery sent to masked recipient");
    }
}
