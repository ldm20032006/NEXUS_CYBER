package demo.server.payment;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@EnableConfigurationProperties(PaymentProperties.class)
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    private final PaymentProperties properties;

    public MockPaymentGatewayAdapter(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentGatewayTopUp startTopUp(UUID userId, BigDecimal amount, String currency) {
        String providerTransactionId = "mock_" + UUID.randomUUID();
        return new PaymentGatewayTopUp(providerName(), providerTransactionId,
                "/mock-payment/topups/" + providerTransactionId);
    }

    @Override
    public String providerName() {
        return properties.provider();
    }

    @Override
    public String adapterMode() {
        return "MOCK_DEVELOPMENT";
    }
}
