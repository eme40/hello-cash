package com.wallet.hello_cash_wallet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.hello_cash_wallet.enums.TransactionType;
import com.wallet.hello_cash_wallet.enums.TransferType;
import com.wallet.hello_cash_wallet.exception.AccountNotFoundException;
import com.wallet.hello_cash_wallet.exception.ErrorObject;
import com.wallet.hello_cash_wallet.exception.UserIdNotFoundException;
import com.wallet.hello_cash_wallet.payload.request.RegistrationRequest;
import com.wallet.hello_cash_wallet.payload.request.TransactionRequest;
import com.wallet.hello_cash_wallet.payload.response.AccountInfo;
import com.wallet.hello_cash_wallet.payload.response.RegistrationResponse;
import com.wallet.hello_cash_wallet.payload.response.TransactionsResponse;
import com.wallet.hello_cash_wallet.service.PayStackService;
import com.wallet.hello_cash_wallet.service.TransactionService;
import com.wallet.hello_cash_wallet.service.UserEntityService;
import com.wallet.hello_cash_wallet.service.WalletService;
import com.wallet.hello_cash_wallet.service.impl.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

  private final TransactionService transactionService;
  private final UserEntityService userEntityService;
  private final WalletService walletService;
  private final PayStackService payStackService;
  private final BankService bankService;
  private final ObjectMapper objectMapper;

  private final Map<String, RegistrationRequest> registrationRequests = new HashMap<>();
  private final Map<String, Integer> registrationSteps = new HashMap<>();

  private final Map<String, TransactionRequest> transactionRequests = new HashMap<>();
  private final Map<String, Integer> transactionSteps = new HashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sendMenuInstructions(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    String payload = message.getPayload().trim();
    String sessionId = session.getId();

    switch (payload) {
      case "1":
        startRegistration(session, sessionId);
        break;
      case "2":
        startTransaction(session, sessionId, TransactionType.TRANSFER);
        session.sendMessage(new TextMessage("Do you want to transfer to (A) HelloCash user or (B) Other bank? Please enter A or B:"));
        break;
      case "3":
        startTransaction(session, sessionId, TransactionType.BUY_CARD);
        session.sendMessage(new TextMessage("Please enter the phone number to buy airtime for:"));
        break;
      case "4":
        startTransaction(session, sessionId, TransactionType.BUY_DATA);
        session.sendMessage(new TextMessage("Please enter the phone number to buy data for:"));
        break;
      default:
        processSteps(session, payload, sessionId);
    }
  }

  private void sendMenuInstructions(WebSocketSession session) throws IOException {
    String instructions = "Welcome! Please select an option:\n" +
            "1. Register\n" +
            "2. Transfer\n" +
            "3. Buy Airtime\n" +
            "4. Buy Data\n" +
            "Send the option number (1, 2, 3, or 4) to proceed.";
    session.sendMessage(new TextMessage(instructions));
  }

  private void startRegistration(WebSocketSession session, String sessionId) throws IOException {
    registrationRequests.put(sessionId, new RegistrationRequest());
    registrationSteps.put(sessionId, 1);
    session.sendMessage(new TextMessage("Please enter your full name:"));
  }

  private void startTransaction(WebSocketSession session, String sessionId, TransactionType transactionType) throws IOException {
    transactionRequests.put(sessionId, new TransactionRequest());
    transactionRequests.get(sessionId).setTransactionType(transactionType);
    transactionSteps.put(sessionId, 1);
  }

  private void processSteps(WebSocketSession session, String payload, String sessionId) throws IOException {
    if (registrationRequests.containsKey(sessionId)) {
      processRegistrationSteps(session, payload, sessionId);
    } else if (transactionRequests.containsKey(sessionId)) {
      TransactionRequest request = transactionRequests.get(sessionId);
      TransactionType transactionType = request.getTransactionType();

      if (transactionType == null) {
        session.sendMessage(new TextMessage("Invalid session. Please start over by selecting an option."));
        return;
      }

      switch (transactionType) {
        case TRANSFER:
          processTransferSteps(session, payload, sessionId);
          break;
        case BUY_CARD:
          processBuyAirtimeSteps(session, payload, sessionId);
          break;
        case BUY_DATA:
          processBuyDataSteps(session, payload, sessionId);
          break;
        default:
          session.sendMessage(new TextMessage("Invalid session. Please start over by selecting an option."));
      }
    } else {
      session.sendMessage(new TextMessage("Invalid session. Please start over by selecting an option."));
    }
  }

  private void processRegistrationSteps(WebSocketSession session, String payload, String sessionId) throws IOException {
    RegistrationRequest request = registrationRequests.get(sessionId);
    int step = registrationSteps.getOrDefault(sessionId, 1);

    try {
      switch (step) {
        case 1:
          request.setFullName(payload);
          session.sendMessage(new TextMessage("Please enter your date of birth (YYYY-MM-DD):"));
          registrationSteps.put(sessionId, 2);
          break;
        case 2:
          request.setDateOfBirth(LocalDate.parse(payload));
          session.sendMessage(new TextMessage("Please enter your BVN:"));
          registrationSteps.put(sessionId, 3);
          break;
        case 3:
          request.setBvn(payload);
          session.sendMessage(new TextMessage("Please create Your HelloCash PIN:"));
          registrationSteps.put(sessionId, 4);
          break;
        case 4:
          request.setPin(payload);

          RegistrationResponse registrationResponse = userEntityService.createUser(request);

          String response = "User registered successfully!\n" +
                  "Account Name: " + registrationResponse.getFullName() + "\n" +
                  "Account Number: " + registrationResponse.getVirtualAccountNumber() + "\n" +
                  "Balance: " + registrationResponse.getBalance();
          session.sendMessage(new TextMessage(response));

          // Clean up session data after successful registration
          registrationRequests.remove(sessionId);
          registrationSteps.remove(sessionId);
          break;
        default:
          session.sendMessage(new TextMessage("Invalid registration step. Please start over by selecting an option."));
          registrationRequests.remove(sessionId);
          registrationSteps.remove(sessionId);
      }
    } catch (Exception e) {
      log.error("Error processing registration step: {}", e.getMessage());
      session.sendMessage(new TextMessage("Error processing registration. Please try again."));
    }
  }

  private void processTransferSteps(WebSocketSession session, String payload, String sessionId) throws IOException {
    TransactionRequest request = transactionRequests.get(sessionId);
    int step = transactionSteps.getOrDefault(sessionId, 1);

    try {
      switch (step) {
        case 1:
          if ("A".equalsIgnoreCase(payload)) {
            request.setTransferType(TransferType.HELLOCASH);
            session.sendMessage(new TextMessage("Please enter the source account to be debited:"));
            transactionSteps.put(sessionId, 2);
          } else if ("B".equalsIgnoreCase(payload)) {
            request.setTransferType(TransferType.OTHERS);
            session.sendMessage(new TextMessage("Please enter the source account to be debited:"));
            transactionSteps.put(sessionId, 2);
          } else {
            session.sendMessage(new TextMessage("Invalid choice. Please enter A or B:"));
          }
          break;
        case 2:
          request.setVirtualAccountNumber(payload);
          session.sendMessage(new TextMessage("Please enter the destination account number:"));
          transactionSteps.put(sessionId, 3);
          break;
        case 3:
          request.setDestinationAccount(payload);
          if (request.getTransferType() == TransferType.OTHERS) {
            List<String> bankNames = bankService.getBankNames();
            String bankListMessage = "Please enter the destination bank name:\n" + String.join(",\n ", bankNames);
            session.sendMessage(new TextMessage(bankListMessage));
            transactionSteps.put(sessionId, 4);
          } else {
            AccountInfo accountInfo = walletService.nameEnquiry(payload);
            request.setDestinationAccountName(accountInfo.getAccountName());
            session.sendMessage(new TextMessage("Destination Account Name: " + accountInfo.getAccountName() + "\nPlease enter the amount:"));
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
              session.sendMessage(new TextMessage("Destination Account Name: " + accountInfo.getAccountName() + "\nPlease enter the amount:"));
              transactionSteps.put(sessionId, 5);
            } catch (Exception e) {
              session.sendMessage(new TextMessage("Account validation failed. Please try again."));
            }
          } else {
            session.sendMessage(new TextMessage("Invalid bank name. Please enter a valid bank name:"));
          }
          break;
        case 5:
          request.setAmount(new BigDecimal(payload));
          session.sendMessage(new TextMessage("Please enter your HelloCash PIN:"));
          transactionSteps.put(sessionId, 6);
          break;
        case 6:
          request.setPin(payload);
          TransactionsResponse transactionsResponse = transactionService.performTransaction(request);
          String response = "Transfer successful!\n" +
                  "Amount: " + transactionsResponse.getAmount() + "\n" +
                  "Balance: " + transactionsResponse.getBalance();
          session.sendMessage(new TextMessage(response));
          // Clean up session data after successful transaction
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
          break;
        default:
          session.sendMessage(new TextMessage("Invalid transfer step. Please start over by selecting an option."));
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
      }
    } catch (Exception e) {
      log.error("Error processing transfer step: {}", e.getMessage());
      session.sendMessage(new TextMessage("Error processing transfer. Please try again."));
    }
  }


  private void processBuyAirtimeSteps(WebSocketSession session, String payload, String sessionId) throws IOException {
    TransactionRequest request = transactionRequests.get(sessionId);
    int step = transactionSteps.getOrDefault(sessionId, 1);

    try {
      switch (step) {
        case 1:
          request.setVirtualAccountNumber(payload);
          session.sendMessage(new TextMessage("Please enter the amount of airtime to buy:"));
          transactionSteps.put(sessionId, 2);
          break;
        case 2:
          request.setAmount(new BigDecimal(payload));
          session.sendMessage(new TextMessage("Please enter your PIN:"));
          transactionSteps.put(sessionId, 3);
          break;
        case 3:
          request.setPin(payload);
          TransactionsResponse transactionResponse = transactionService.performTransaction(request);
          sendTransactionResponse(session, transactionResponse);
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
          break;
        default:
          session.sendMessage(new TextMessage("Invalid airtime step. Please start over by selecting an option."));
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
      }
    } catch (Exception e) {
      log.error("Error processing airtime step: {}", e.getMessage());
      sendErrorMessage(session, e);
    }
  }

  private void processBuyDataSteps(WebSocketSession session, String payload, String sessionId) throws IOException {
    TransactionRequest request = transactionRequests.get(sessionId);
    int step = transactionSteps.getOrDefault(sessionId, 1);

    try {
      switch (step) {
        case 1:
          request.setVirtualAccountNumber(payload);
          session.sendMessage(new TextMessage("Please enter the amount of data to buy:"));
          transactionSteps.put(sessionId, 2);
          break;
        case 2:
          request.setAmount(new BigDecimal(payload));
          session.sendMessage(new TextMessage("Please enter your PIN:"));
          transactionSteps.put(sessionId, 3);
          break;
        case 3:
          request.setPin(payload);
          TransactionsResponse transactionResponse = transactionService.performTransaction(request);
          sendTransactionResponse(session, transactionResponse);
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
          break;
        default:
          session.sendMessage(new TextMessage("Invalid data step. Please start over by selecting an option."));
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
      }
    } catch (Exception e) {
      log.error("Error processing data step: {}", e.getMessage());
      sendErrorMessage(session, e);
    }
  }

  private void sendTransactionResponse(WebSocketSession session, TransactionsResponse transactionResponse) throws IOException {
    String responseMessage = "Transaction Response:\n" +
            "Response Code: " + transactionResponse.getStatusCode() + "\n" +
            "Response Message: " + transactionResponse.getMessage();

    session.sendMessage(new TextMessage(responseMessage));
  }

  private void sendErrorMessage(WebSocketSession session, Exception e) throws IOException {
    String errorMessage;
    int statusCode;

    if (e instanceof AccountNotFoundException || e instanceof UserIdNotFoundException) {
      errorMessage = "Account not found. Please check your details and try again.";
      statusCode = HttpStatus.NOT_FOUND.value();
    } else {
      errorMessage = "An unexpected error occurred. Please try again later.";
      statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
      log.error("Unexpected error occurred: {}", e.getMessage(), e);
    }

    ErrorObject errorObject = new ErrorObject(statusCode, errorMessage);
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorObject)));
  }
}

