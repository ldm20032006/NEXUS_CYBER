package demo.server.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGatewayPort {

    PaymentGatewayTopUp startTopUp(UUID userId, BigDecimal amount, String currency);

    String providerName();

    String adapterMode();
}
