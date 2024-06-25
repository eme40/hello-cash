package com.wallet.hello_cash_wallet.service.impl;

import com.wallet.hello_cash_wallet.entities.Transaction;
import com.wallet.hello_cash_wallet.entities.Wallet;
import com.wallet.hello_cash_wallet.enums.TransactionStatus;
import com.wallet.hello_cash_wallet.enums.TransactionType;
import com.wallet.hello_cash_wallet.enums.TransferType;
import com.wallet.hello_cash_wallet.exception.AccountNotFoundException;
import com.wallet.hello_cash_wallet.payload.request.TransactionRequest;
import com.wallet.hello_cash_wallet.payload.response.AccountInfo;
import com.wallet.hello_cash_wallet.payload.response.TransactionsResponse;
import com.wallet.hello_cash_wallet.repository.TransactionRepository;
import com.wallet.hello_cash_wallet.repository.UserEntityRepository;
import com.wallet.hello_cash_wallet.repository.WalletRepository;
import com.wallet.hello_cash_wallet.service.PayStackService;
import com.wallet.hello_cash_wallet.service.TransactionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

  private final UserEntityRepository userEntityRepository;
  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final PayStackService payStackService;

  public TransactionServiceImpl(UserEntityRepository userEntityRepository, WalletRepository walletRepository,
                                TransactionRepository transactionRepository, PayStackService payStackService) {
    this.userEntityRepository = userEntityRepository;
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.payStackService = payStackService;
  }

  @Override
  @Transactional
  public TransactionsResponse performTransaction(TransactionRequest request) {
    try {
      log.info("Performing transaction: {}", request);
      Wallet wallet = walletRepository.findByVirtualAccountNumber(request.getVirtualAccountNumber());
      if (wallet == null) {
        log.error("Account not found for virtual account number: {}", request.getVirtualAccountNumber());
        throw new AccountNotFoundException("Account not found");
      }

      if (!wallet.getUser().getPin().equals(request.getPin())) {
        log.error("Invalid PIN for user with virtual account number: {}", request.getVirtualAccountNumber());
        return TransactionsResponse.builder()
                .message("Invalid Pin")
                .statusCode(400)
                .build();
      }

      // Process transaction based on action
      switch (request.getTransactionType()) {
        case BUY_CARD:
          // return handleCredit(wallet, request.getAmount());
        case BUY_DATA:
          // return handleDebit(wallet, request.getAmount());
        case TRANSFER:
          return handleTransfer(request);
        default:
          log.error("Invalid transaction type: {}", request.getTransactionType());
          return TransactionsResponse.builder()
                  .statusCode(404)
                  .message("Invalid action")
                  .build();
      }
    } catch (Exception e) {
      log.error("Error performing transaction: {}", e.getMessage(), e);
      throw new RuntimeException("Transaction failed", e);
    }
  }


  @Transactional
  public TransactionsResponse handleTransfer(TransactionRequest request) {
    try {
      Wallet sourceWallet = walletRepository.findByVirtualAccountNumber(request.getVirtualAccountNumber());
      if (sourceWallet == null) {
        log.error("Source account not found for virtual account number: {}", request.getVirtualAccountNumber());
        throw new AccountNotFoundException("Source account not found");
      }

      log.info("Source Wallet before transaction: {}", sourceWallet.getBalance());

      if (request.getTransferType().equals(TransferType.HELLOCASH)) {
        Wallet destinationWallet = walletRepository.findByVirtualAccountNumber(request.getDestinationAccount());
        if (destinationWallet == null) {
          log.error("Destination account not found for virtual account number: {}", request.getDestinationAccount());
          throw new AccountNotFoundException("Destination account not found");
        }
        if (request.getAmount().compareTo(sourceWallet.getBalance()) > 0) {
          log.error("Insufficient balance for virtual account number: {}. Transfer amount: {}, Balance: {}",
                  request.getVirtualAccountNumber(), request.getAmount(), sourceWallet.getBalance());
          return TransactionsResponse.builder()
                  .statusCode(200)
                  .message("Insufficient balance")
                  .amount(request.getAmount())
                  .balance(sourceWallet.getBalance())
                  .build();
        }

        // Deduct from source account
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(sourceWallet);
        log.info("Source Wallet after deduction: {}", sourceWallet.getBalance());
        saveTransaction(sourceWallet, request.getAmount(), TransactionType.TRANSFER, TransactionStatus.SUCCESS);

        // Add to destination account
        destinationWallet.setBalance(destinationWallet.getBalance().add(request.getAmount()));
        walletRepository.save(destinationWallet);
        log.info("Destination Wallet after addition: {}", destinationWallet.getBalance());
        saveTransaction(destinationWallet, request.getAmount(), TransactionType.TRANSFER, TransactionStatus.SUCCESS);

        return TransactionsResponse.builder()
                .statusCode(200)
                .message("Transfer successful")
                .amount(request.getAmount())
                .balance(sourceWallet.getBalance())
                .build();
      } else {
        if (request.getBankCode() == null || request.getBankCode().isEmpty()) {
          log.error("Bank code is null or empty");
          return TransactionsResponse.builder()
                  .statusCode(400)
                  .message("Invalid bank code")
                  .amount(request.getAmount())
                  .balance(sourceWallet.getBalance())
                  .build();
        }

        log.info("Bank code: {}", request.getBankCode());

        if (!payStackService.isValidBankCode(request.getBankCode())) {
          log.error("Invalid bank code: {}", request.getBankCode());
          return TransactionsResponse.builder()
                  .statusCode(400)
                  .message("Invalid bank code")
                  .amount(request.getAmount())
                  .balance(sourceWallet.getBalance())
                  .build();
        }

        AccountInfo accountInfo = payStackService.validateAccount(request.getDestinationAccount(), request.getBankCode());
        if (accountInfo == null || accountInfo.getAccountName() == null) {
          log.error("Invalid destination account details for account number: {} and bank code: {}",
                  request.getDestinationAccount(), request.getBankCode());
          return TransactionsResponse.builder()
                  .statusCode(400)
                  .message("Invalid destination account details")
                  .amount(request.getAmount())
                  .balance(sourceWallet.getBalance())
                  .build();
        }

        if (request.getAmount().compareTo(sourceWallet.getBalance()) > 0) {
          log.error("Insufficient balance for virtual account number: {}. Transfer amount: {}, Balance: {}",
                  request.getVirtualAccountNumber(), request.getAmount(), sourceWallet.getBalance());
          return TransactionsResponse.builder()
                  .statusCode(200)
                  .message("Insufficient balance")
                  .amount(request.getAmount())
                  .balance(sourceWallet.getBalance())
                  .build();
        }

        // Deduct from source account
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(sourceWallet);
        log.info("Source Wallet after deduction: {}", sourceWallet.getBalance());
        saveTransaction(sourceWallet, request.getAmount(), TransactionType.TRANSFER, TransactionStatus.SUCCESS);

        log.info("Transferred {} to {} at bank code {}. Destination account name: {}",
                request.getAmount(), request.getDestinationAccount(), request.getBankCode(), accountInfo.getAccountName());

        return TransactionsResponse.builder()
                .statusCode(200)
                .message("Transfer successful")
                .amount(request.getAmount())
                .balance(sourceWallet.getBalance())
                .build();
      }
    } catch (Exception e) {
      log.error("Error handling transfer: {}", e.getMessage(), e);
      throw new RuntimeException("Transfer failed", e);
    }
  }

  private void saveTransaction(Wallet wallet, BigDecimal amount, TransactionType type, TransactionStatus status) {
    try {
      Transaction transaction = Transaction.builder()
              .transactionType(type)
              .virtualAccountNumber(wallet)
              .amount(amount)
              .transactionStatus(status)
              .build();
      transactionRepository.save(transaction);
      log.info("Transaction saved: {}", transaction);
    } catch (Exception e) {
      log.error("Error saving transaction: {}", e.getMessage(), e);
      throw new RuntimeException("Transaction save failed", e);
    }
  }
}