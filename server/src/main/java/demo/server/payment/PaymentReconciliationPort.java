package demo.server.payment;

import demo.server.common.enums.PaymentTransactionStatus;
import demo.server.entity.payment.PaymentTransaction;

public interface PaymentReconciliationPort {

    PaymentTransactionStatus reconcile(PaymentTransaction transaction);
}
