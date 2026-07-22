package demo.server.service.wallet;

import demo.server.dto.wallet.WalletResponse;
import demo.server.dto.wallet.WalletTransactionResponse;
import demo.server.entity.wallet.Wallet;
import demo.server.entity.wallet.WalletTransaction;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public WalletResponse toWallet(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getUser().getId(), wallet.getBalance(), wallet.getCurrency());
    }

    public WalletTransactionResponse toTransaction(WalletTransaction transaction) {
        return new WalletTransactionResponse(transaction.getId(), transaction.getWallet().getId(),
                transaction.getUser().getId(), transaction.getType(), transaction.getAmount(), transaction.getCurrency(),
                transaction.getBalanceBefore(), transaction.getBalanceAfter(), transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getOriginalTransaction() == null ? null : transaction.getOriginalTransaction().getId(),
                transaction.getDescription(), transaction.getCreatedAt());
    }
}
