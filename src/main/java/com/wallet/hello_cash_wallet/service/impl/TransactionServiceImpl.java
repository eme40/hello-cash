package com.wallet.hello_cash_wallet.service.impl;

import com.wallet.hello_cash_wallet.entities.Transaction;
import com.wallet.hello_cash_wallet.entities.UserEntity;
import com.wallet.hello_cash_wallet.entities.Wallet;
import com.wallet.hello_cash_wallet.enums.TransactionStatus;
import com.wallet.hello_cash_wallet.enums.TransactionType;
import com.wallet.hello_cash_wallet.exception.AccountNotFoundException;
import com.wallet.hello_cash_wallet.exception.UserIdNotFoundException;
import com.wallet.hello_cash_wallet.payload.request.TransactionRequest;
import com.wallet.hello_cash_wallet.payload.response.TransactionsResponse;
import com.wallet.hello_cash_wallet.repository.TransactionRepository;
import com.wallet.hello_cash_wallet.repository.UserEntityRepository;
import com.wallet.hello_cash_wallet.repository.WalletRepository;
import com.wallet.hello_cash_wallet.service.TransactionService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@AllArgsConstructor
@Service

public class TransactionServiceImpl implements TransactionService {

  private final UserEntityRepository userEntityRepository;
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;

  @Transactional
  public TransactionsResponse performTransaction(TransactionRequest request) {
//    UserEntity user = userEntityRepository.findById(request.getId()).orElseThrow(() -> new UserIdNotFoundException("user not found"));
    Wallet wallet = walletRepository.findByVirtualAccountNumber(request.getVirtualAccountNumber());
    if (wallet == null) {
      throw new AccountNotFoundException("Account not found");
    }
    if (!wallet.getUser().getPin().equals(request.getPin())){
      return TransactionsResponse.builder()
              .message("Invalid Pin")
              .StatusCode(400)
              .build();
    }
    // Process transaction based on action
    switch (request.getTransactionType()) {
      case CREDIT:
        return handleCredit(wallet, request.getAmount());
      case DEBIT:
        return handleDebit(wallet, request.getAmount());
      case TRANSFER:
        return handleTransfer(request);
      default:
        return TransactionsResponse.builder()
                .StatusCode(404)
                .message("Invalid action")
                .build();
    }
  }

  private TransactionsResponse handleCredit(Wallet wallet, BigDecimal amount) {
    wallet.credit(amount);
    walletRepository.save(wallet);
    saveTransaction(wallet, amount, TransactionType.CREDIT, TransactionStatus.SUCCESS);
    return TransactionsResponse.builder()
            .StatusCode(200)
            .message("Credited " + amount + " to account. New balance: " + wallet.getBalance())
            .build();
  }

  private TransactionsResponse handleDebit(Wallet wallet, BigDecimal amount) {
    if (wallet.getBalance().compareTo(amount) >= 0) {
      wallet.debit(amount);
      walletRepository.save(wallet);
      saveTransaction(wallet, amount, TransactionType.DEBIT, TransactionStatus.SUCCESS);
      return TransactionsResponse.builder()
              .StatusCode(200)
              .message("Debited " + amount + " from account. New balance: " + wallet.getBalance())
              .build();
    } else {
      return TransactionsResponse.builder()
              .StatusCode(200)
              .message("Insufficient funds")
              .build();
    }
  }

  private TransactionsResponse handleTransfer(TransactionRequest request) {
    Wallet sourceWallet = walletRepository.findByVirtualAccountNumber(request.getVirtualAccountNumber());
    Wallet destinationWallet = walletRepository.findByVirtualAccountNumber(request.getDestinationAccount());

    if (destinationWallet == null) {
      throw new AccountNotFoundException("Destination account not found");
    }

    if (request.getAmount().compareTo(sourceWallet.getBalance()) > 0) {
      return TransactionsResponse.builder()
              .StatusCode(200)
              .message("Insufficient balance")
              .build();
    }

    // Deduct from source account
    sourceWallet.setBalance(sourceWallet.getBalance().subtract(request.getAmount()));
    walletRepository.save(sourceWallet);
    saveTransaction(sourceWallet, request.getAmount(), TransactionType.DEBIT, TransactionStatus.SUCCESS);

    // Add to destination account
    destinationWallet.setBalance(destinationWallet.getBalance().add(request.getAmount()));
    walletRepository.save(destinationWallet);
    saveTransaction(destinationWallet, request.getAmount(), TransactionType.CREDIT, TransactionStatus.SUCCESS);

    return TransactionsResponse.builder()
            .StatusCode(200)
            .message("Transfer successful")
            .build();
  }

  private void saveTransaction(Wallet wallet, BigDecimal amount, TransactionType type, TransactionStatus status) {
    Transaction transaction = Transaction.builder()
            .transactionType(type)
            .virtualAccountNumber(wallet)
            .amount(amount)
            .transactionStatus(status)
            .build();
    transactionRepository.save(transaction);
  }
}
