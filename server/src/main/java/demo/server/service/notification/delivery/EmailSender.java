package demo.server.service.notification.delivery;

import demo.server.entity.notification.Notification;

public interface EmailSender {

    DeliverySendResult sendAccountSecurityEmail(String recipientEmail, Notification notification);
}
