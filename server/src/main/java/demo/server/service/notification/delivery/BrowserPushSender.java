package demo.server.service.notification.delivery;

import demo.server.entity.notification.Notification;
import demo.server.entity.notification.PushSubscription;

public interface BrowserPushSender {

    DeliverySendResult send(Notification notification, PushSubscription subscription);
}
