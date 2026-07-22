package demo.server.service.payment;

import demo.server.dto.payment.PaymentTransactionResponse;
import demo.server.dto.payment.TopUpResponse;
import demo.server.entity.payment.PaymentTransaction;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public TopUpResponse toTopUp(PaymentTransaction transaction, String adapterMode) {
        return new TopUpResponse(transaction.getId(), transaction.getProvider(), transaction.getProviderTransactionId(),
                transaction.getStatus(), transaction.getAmount(), transaction.getCurrency(), transaction.getCheckoutUrl(),
                adapterMode, transaction.getRequestedAt());
    }

    public PaymentTransactionResponse toTransaction(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(transaction.getId(), transaction.getUser().getId(),
                transaction.getProvider(), transaction.getProviderTransactionId(), transaction.getStatus(),
                transaction.getAmount(), transaction.getCurrency(),
                transaction.getWalletTransaction() == null ? null : transaction.getWalletTransaction().getId(),
                transaction.getRequestedAt(), transaction.getProcessedAt(), transaction.getFailureReason());
    }
}
