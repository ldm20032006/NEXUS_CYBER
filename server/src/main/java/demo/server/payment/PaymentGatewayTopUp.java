package demo.server.payment;

public record PaymentGatewayTopUp(
        String provider,
        String providerTransactionId,
        String checkoutUrl
) {
}
