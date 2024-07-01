package com.wallet.hello_cash_wallet.config;

import com.wallet.hello_cash_wallet.enums.TransactionType;
import com.wallet.hello_cash_wallet.enums.TransferType;
import com.wallet.hello_cash_wallet.payload.request.RegistrationRequest;
import com.wallet.hello_cash_wallet.payload.request.TransactionRequest;
import com.wallet.hello_cash_wallet.payload.response.AccountInfo;
import com.wallet.hello_cash_wallet.payload.response.RegistrationResponse;
import com.wallet.hello_cash_wallet.payload.response.TransactionsResponse;
import com.wallet.hello_cash_wallet.service.*;
import com.wallet.hello_cash_wallet.service.impl.BankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SMSHandlerService {

  private final UserEntityService userEntityService;
  private final TransactionService transactionService;
  private final TwilioService twilioService;
  private final BankService bankService;
  private final WalletService walletService;
  private final PayStackService payStackService;


  private final Map<String, RegistrationRequest> registrationRequests = new HashMap<>();
  private final Map<String, Integer> registrationSteps = new HashMap<>();
  private final Map<String, TransactionRequest> transactionRequests = new HashMap<>();
  private final Map<String, Integer> transactionSteps = new HashMap<>();

  public SMSHandlerService(UserEntityService userEntityService, TransactionService transactionService,
                           TwilioService twilioService, BankService bankService, WalletService walletService, PayStackService payStackService) {
    this.userEntityService = userEntityService;
    this.transactionService = transactionService;
    this.twilioService = twilioService;
    this.bankService = bankService;
    this.walletService = walletService;
    this.payStackService = payStackService;
  }

  public void handleIncomingSms(String fromPhoneNumber, String message) {
    String sessionId = fromPhoneNumber;
    log.info("Received SMS from {}: {}", fromPhoneNumber, message);

    switch (message.trim()) {
      case "1":
        startRegistration(fromPhoneNumber, sessionId);
        break;
      case "2":
        startTransaction(fromPhoneNumber, sessionId, TransactionType.TRANSFER);
        break;
      case "3":
        startTransaction(fromPhoneNumber, sessionId, TransactionType.BUY_CARD);
        break;
      case "4":
        startTransaction(fromPhoneNumber, sessionId, TransactionType.BUY_DATA);
        break;
      default:
        processSteps(fromPhoneNumber, message, sessionId);
    }
  }
  private void startRegistration(String fromPhoneNumber, String sessionId) {
    registrationRequests.put(sessionId, new RegistrationRequest());
    registrationSteps.put(sessionId, 1);
    log.info("Starting registration for {}", fromPhoneNumber);
    twilioService.sendSms(fromPhoneNumber, "Please enter your full name:");
  }

  private void startTransaction(String fromPhoneNumber, String sessionId, TransactionType transactionType) {
    transactionRequests.put(sessionId, new TransactionRequest());
    transactionRequests.get(sessionId).setTransactionType(transactionType);
    transactionSteps.put(sessionId, 1);
  }

  private void processSteps(String fromPhoneNumber, String payload, String sessionId) {
    if (registrationRequests.containsKey(sessionId)) {
      processRegistrationSteps(fromPhoneNumber, payload, sessionId);
    } else if (transactionRequests.containsKey(sessionId)) {
      TransactionRequest request = transactionRequests.get(sessionId);
      TransactionType transactionType = request.getTransactionType();

      if (transactionType == null) {
        twilioService.sendSms(fromPhoneNumber, "Invalid session. Please start over by selecting an option.");
        return;
      }

      switch (transactionType) {
        case TRANSFER:
          processTransferSteps(fromPhoneNumber, payload, sessionId);
          break;
        case BUY_CARD:
//          processBuyAirtimeSteps(fromPhoneNumber, payload, sessionId);
          break;
        case BUY_DATA:
//          processBuyDataSteps(fromPhoneNumber, payload, sessionId);
          break;
        default:
          twilioService.sendSms(fromPhoneNumber, "Invalid session. Please start over by selecting an option.");
      }
    } else {
      twilioService.sendSms(fromPhoneNumber, "Invalid session. Please start over by selecting an option.");
    }
  }

  private void processRegistrationSteps(String fromPhoneNumber, String payload, String sessionId) {
    RegistrationRequest request = registrationRequests.get(sessionId);
    int step = registrationSteps.getOrDefault(sessionId, 1);
    log.info("Processing registration step {} for {}", step, fromPhoneNumber);

    try {
      switch (step) {
        case 1:
          if (!payload.matches("[A-Za-z ]+")) {
            twilioService.sendSms(fromPhoneNumber, "Invalid name format. Please enter your full name using letters only.");
            return;
          }
          request.setFullName(payload);
          log.info("Received full name: {}", payload);
          twilioService.sendSms(fromPhoneNumber, "Please enter your date of birth (YYYY-MM-DD):");
          registrationSteps.put(sessionId, 2);
          break;
        case 2:
          try {
            LocalDate dateOfBirth = LocalDate.parse(payload);
            if (dateOfBirth.isAfter(LocalDate.now())) {
              throw new DateTimeParseException("Future date", payload, 0);
            }
            request.setDateOfBirth(dateOfBirth);
            log.info("Received date of birth: {}", payload);
            twilioService.sendSms(fromPhoneNumber, "Please enter your BVN:");
            registrationSteps.put(sessionId, 3);
          } catch (DateTimeParseException e) {
            twilioService.sendSms(fromPhoneNumber, "Invalid date format. Please enter your date of birth (YYYY-MM-DD):");
          }
          break;
        case 3:
          if (!payload.matches("\\d{11}")) {
            twilioService.sendSms(fromPhoneNumber, "Invalid BVN. Please enter an 11-digit BVN:");
            return;
          }
          request.setBvn(payload);
          log.info("Received BVN: {}", payload);
          twilioService.sendSms(fromPhoneNumber, "Please enter your phone number:");
          registrationSteps.put(sessionId, 4);
          break;
        case 4:
          if (!payload.matches("\\d{10,15}")) {
            twilioService.sendSms(fromPhoneNumber, "Invalid phone number. Please enter a valid phone number:");
            return;
          }
          request.setPhoneNumber(payload);
          log.info("Received phone number: {}", payload);
          twilioService.sendSms(fromPhoneNumber, "Please create Your HelloCash PIN:");
          registrationSteps.put(sessionId, 5);
          break;
        case 5:
          if (!payload.matches("\\d{4}")) {
            twilioService.sendSms(fromPhoneNumber, "Invalid PIN. Please enter a 4-digit PIN:");
            return;
          }
          request.setPin(payload);
          log.info("Received PIN: {}", payload);

          RegistrationResponse registrationResponse = userEntityService.createUser(request);

          String response = "User registered successfully!\n" +
                  "Account Name: " + registrationResponse.getFullName() + "\n" +
                  "Account Number: " + registrationResponse.getVirtualAccountNumber() + "\n" +
                  "Balance: " + registrationResponse.getBalance();
          twilioService.sendSms(fromPhoneNumber, response);

          // Clean up session data after successful registration
          registrationRequests.remove(sessionId);
          registrationSteps.remove(sessionId);
          break;
        default:
          twilioService.sendSms(fromPhoneNumber, "Invalid registration step. Please start over by selecting an option.");
          registrationRequests.remove(sessionId);
          registrationSteps.remove(sessionId);
      }
    } catch (Exception e) {
      log.error("Error processing registration step: {}", e.getMessage());
      twilioService.sendSms(fromPhoneNumber, "Error processing registration. Please try again.");
    }
  }


  private void processTransferSteps(String fromPhoneNumber, String payload, String sessionId) {
    TransactionRequest request = transactionRequests.get(sessionId);
    int step = transactionSteps.getOrDefault(sessionId, 1);

    try {
      switch (step) {
        case 1:
          if ("A".equalsIgnoreCase(payload)) {
            request.setTransferType(TransferType.HELLOCASH);
            setSourceAccountFromPhoneNumber(fromPhoneNumber, request);
            twilioService.sendSms(fromPhoneNumber, "Please enter the destination account number:");
            transactionSteps.put(sessionId, 3);
          } else if ("B".equalsIgnoreCase(payload)) {
            request.setTransferType(TransferType.OTHERS);
            setSourceAccountFromPhoneNumber(fromPhoneNumber, request);
            twilioService.sendSms(fromPhoneNumber, "Please enter the destination account number:");
            transactionSteps.put(sessionId, 3);
          } else {
            twilioService.sendSms(fromPhoneNumber, "Invalid choice. Please enter A or B:");
          }
          break;
        case 3:
          request.setDestinationAccount(payload);
          if (request.getTransferType() == TransferType.OTHERS) {
            List<String> bankNames = bankService.getBankNames();
            String bankListMessage = "Please enter the destination bank name:\n" + String.join(",\n ", bankNames);
            twilioService.sendSms(fromPhoneNumber, bankListMessage);
            transactionSteps.put(sessionId, 4);
          } else {
            AccountInfo accountInfo = walletService.nameEnquiry(payload);
            request.setDestinationAccountName(accountInfo.getAccountName());
            twilioService.sendSms(fromPhoneNumber, "Destination Account Name: " + accountInfo.getAccountName() + "\nPlease enter the amount:");
            transactionSteps.put(sessionId, 5);
          }
          break;
        case 4:
          String bankName = payload.trim();
          if (bankService.isValidBankName(bankName)) {
            String bankCode = bankService.getBankCode(bankName);
            log.info("Bank code for {} is {}", bankName, bankCode);
            request.setBankCode(bankCode);
            try {
              AccountInfo accountInfo = payStackService.validateAccount(request.getDestinationAccount(), bankCode);
              request.setDestinationAccountName(accountInfo.getAccountName());
              twilioService.sendSms(fromPhoneNumber, "Destination Account Name: " + accountInfo.getAccountName() + "\nPlease enter the amount:");
              transactionSteps.put(sessionId, 5);
            } catch (Exception e) {
              twilioService.sendSms(fromPhoneNumber, "Account validation failed. Please try again.");
            }
          } else {
            twilioService.sendSms(fromPhoneNumber, "Invalid bank name. Please enter a valid bank name:");
          }
          break;
        case 5:
          request.setAmount(new BigDecimal(payload));
          twilioService.sendSms(fromPhoneNumber, "Please enter your HelloCash PIN:");
          transactionSteps.put(sessionId, 6);
          break;
        case 6:
          request.setPin(payload);
          TransactionsResponse transactionsResponse = transactionService.handleTransfer(request);

          if (transactionsResponse.getStatusCode() == 200) {  // Check for HTTP 200 OK status
            twilioService.sendSms(fromPhoneNumber, transactionsResponse.getMessage());
          } else {
            twilioService.sendSms(fromPhoneNumber, "Transaction failed: " + transactionsResponse.getMessage());
          }

          // Clean up session data after transaction
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
          break;
        default:
          twilioService.sendSms(fromPhoneNumber, "Invalid transfer step. Please start over by selecting an option.");
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
      }
    } catch (Exception e) {
      log.error("Error processing transfer step: {}", e.getMessage());
      twilioService.sendSms(fromPhoneNumber, "Error processing transfer. Please try again.");
    }
  }


//  private void processBuyAirtimeSteps(String fromPhoneNumber, String payload, String sessionId) {
//    TransactionRequest request = transactionRequests.get(sessionId);
//    int step = transactionSteps.getOrDefault(sessionId, 1);
//
//    try {
//      switch (step) {
//        case 1:
//          request.setPhoneNumber(payload);
//          setSourceAccountFromPhoneNumber(fromPhoneNumber, request);
//          twilioService.sendSms(fromPhoneNumber, "Please enter the amount:");
//          transactionSteps.put(sessionId, 3);
//          break;
//        case 3:
//          request.setAmount(new BigDecimal(payload));
//          twilioService.sendSms(fromPhoneNumber, "Please enter your HelloCash PIN:");
//          transactionSteps.put(sessionId, 4);
//          break;
//        case 4:
//          request.setPin(payload);
//          TransactionsResponse transactionsResponse = transactionService.buyAirtime(request);
//
//          if ("00".equals(transactionsResponse.getResponseCode())) {
//            twilioService.sendSms(fromPhoneNumber, transactionsResponse.getResponseMessage());
//          } else {
//            twilioService.sendSms(fromPhoneNumber, "Transaction failed: " + transactionsResponse.getResponseMessage());
//          }
//
//          // Clean up session data after successful transaction
//          transactionRequests.remove(sessionId);
//          transactionSteps.remove(sessionId);
//          break;
//        default:
//          twilioService.sendSms(fromPhoneNumber, "Invalid buy airtime step. Please start over by selecting an option.");
//          transactionRequests.remove(sessionId);
//          transactionSteps.remove(sessionId);
//      }
//    } catch (Exception e) {
//      log.error("Error processing buy airtime step: {}", e.getMessage());
//      twilioService.sendSms(fromPhoneNumber, "Error processing buy airtime. Please try again.");
//    }
//  }

//  private void processBuyDataSteps(String fromPhoneNumber, String payload, String sessionId) {
//    TransactionRequest request = transactionRequests.get(sessionId);
//    int step = transactionSteps.getOrDefault(sessionId, 1);
//
//    try {
//      switch (step) {
//        case 1:
//          request.setPhoneNumber(payload);
//          setSourceAccountFromPhoneNumber(fromPhoneNumber, request);
//          twilioService.sendSms(fromPhoneNumber, "Please enter the amount:");
//          transactionSteps.put(sessionId, 3);
//          break;
//        case 3:
//          request.setAmount(new BigDecimal(payload));
//          twilioService.sendSms(fromPhoneNumber, "Please enter your HelloCash PIN:");
//          transactionSteps.put(sessionId, 4);
//          break;
//        case 4:
//          request.setPin(payload);
//          TransactionsResponse transactionsResponse = transactionService.buyData(request);
//
//          if ("00".equals(transactionsResponse.getResponseCode())) {
//            twilioService.sendSms(fromPhoneNumber, transactionsResponse.getResponseMessage());
//          } else {
//            twilioService.sendSms(fromPhoneNumber, "Transaction failed: " + transactionsResponse.getResponseMessage());
//          }
//
//          // Clean up session data after successful transaction
//          transactionRequests.remove(sessionId);
//          transactionSteps.remove(sessionId);
//          break;
//        default:
//          twilioService.sendSms(fromPhoneNumber, "Invalid buy data step. Please start over by selecting an option.");
//          transactionRequests.remove(sessionId);
//          transactionSteps.remove(sessionId);
//      }
//    } catch (Exception e) {
//      log.error("Error processing buy data step: {}", e.getMessage());
//      twilioService.sendSms(fromPhoneNumber, "Error processing buy data. Please try again.");
//    }
//  }

  private void setSourceAccountFromPhoneNumber(String fromPhoneNumber, TransactionRequest request) {
    String virtualAccountNumber = userEntityService.getAccountNumberByPhoneNumber(fromPhoneNumber);
    request.setVirtualAccountNumber(virtualAccountNumber);
  }
}

