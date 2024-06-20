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
import com.wallet.hello_cash_wallet.service.TransactionService;
import com.wallet.hello_cash_wallet.service.UserEntityService;
import com.wallet.hello_cash_wallet.service.WalletService;
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
import java.util.Map;

@Component
//@AllArgsConstructor
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

  private final TransactionService transactionService;
  private final UserEntityService userEntityService;
  private final WalletService walletService;

  // Maps to keep track of registration and transaction state for each session
  private final Map<String, RegistrationRequest> registrationRequests = new HashMap<>();
  private final Map<String, Integer> registrationSteps = new HashMap<>();

  private final Map<String, TransactionRequest> transactionRequests = new HashMap<>();
  private final Map<String, Integer> transactionSteps = new HashMap<>();

  private final ObjectMapper objectMapper;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sendMenuInstructions(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    String payload = message.getPayload().trim();
    String sessionId = session.getId();

    if ("1".equals(payload)) {
      registrationRequests.put(sessionId, new RegistrationRequest());
      registrationSteps.put(sessionId, 1);
      session.sendMessage(new TextMessage("Please enter your full name:"));
    } else if ("2".equals(payload)) {
      transactionRequests.put(sessionId, new TransactionRequest());
      transactionSteps.put(sessionId, 1);
      session.sendMessage(new TextMessage("Please enter your action (credit, debit, transfer):"));
    } else {
      processSteps(session, payload, sessionId);
    }
  }

  private void sendMenuInstructions(WebSocketSession session) throws IOException {
    String instructions = "Welcome! Please select an option:\n" +
            "1. Register\n" +
            "2. Perform a transaction\n" +
            "Send the option number (1 or 2) to proceed.";
    session.sendMessage(new TextMessage(instructions));
  }

  private void processSteps(WebSocketSession session, String payload, String sessionId) throws IOException {
    if (registrationRequests.containsKey(sessionId)) {
      processRegistrationSteps(session, payload, sessionId);
    } else if (transactionRequests.containsKey(sessionId)) {
      processTransactionSteps(session, payload, sessionId);
    } else {
      session.sendMessage(new TextMessage("Invalid session. Please start over by selecting an option."));
    }
  }

  private void processRegistrationSteps(WebSocketSession session, String payload, String sessionId) throws IOException {
    if (!registrationRequests.containsKey(sessionId)) {
      session.sendMessage(new TextMessage("Invalid session. Please start over by selecting an option."));
      return;
    }

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

  private void processTransactionSteps(WebSocketSession session, String payload, String sessionId) throws IOException {
    TransactionRequest request = transactionRequests.get(sessionId);
    int step = transactionSteps.getOrDefault(sessionId, 1);

    try {
      switch (step) {
        case 1:
          if ("credit".equalsIgnoreCase(payload)) {
            request.setTransactionType(TransactionType.CREDIT);
            session.sendMessage(new TextMessage("Please enter the account number to credit:"));
            transactionSteps.put(sessionId, 2);
          } else if ("debit".equalsIgnoreCase(payload)) {
            request.setTransactionType(TransactionType.DEBIT);
            session.sendMessage(new TextMessage("Please enter the account number to debit:"));
            transactionSteps.put(sessionId, 2);
          } else if ("transfer".equalsIgnoreCase(payload)) {
            request.setTransactionType(TransactionType.TRANSFER);
            session.sendMessage(new TextMessage("Do you want to transfer to (A) HelloCash user or (B) Other bank? Please enter A or B:"));
            transactionSteps.put(sessionId, 2);
          } else {
            session.sendMessage(new TextMessage("Invalid transaction type. Please enter credit, debit, or transfer:"));
          }
          break;
        case 2:
          if (request.getTransactionType() == TransactionType.TRANSFER) {
            if ("A".equalsIgnoreCase(payload)) {
              request.setTransferType(TransferType.HELLOCASH);
              session.sendMessage(new TextMessage("Please enter the source account to be debited:"));
              transactionSteps.put(sessionId, 3);
            } else if ("B".equalsIgnoreCase(payload)) {
              request.setTransferType(TransferType.OTHERS);
              session.sendMessage(new TextMessage("Please enter the source account to be debited:"));
              transactionSteps.put(sessionId, 3);
            } else {
              session.sendMessage(new TextMessage("Invalid choice. Please enter A or B:"));
            }
          } else {
            request.setVirtualAccountNumber(payload);
            session.sendMessage(new TextMessage("Please enter the amount:"));
            transactionSteps.put(sessionId, 3);
          }
          break;
        case 3:
          if (request.getTransactionType() == TransactionType.TRANSFER) {
            request.setVirtualAccountNumber(payload);
            if (request.getTransferType() == TransferType.HELLOCASH) {
              session.sendMessage(new TextMessage("Please enter the destination HelloCash account number:"));
              transactionSteps.put(sessionId, 4);
            } else if (request.getTransferType() == TransferType.OTHERS) {
              session.sendMessage(new TextMessage("Please enter the destination bank name:"));
              transactionSteps.put(sessionId, 4);
            }
          } else {
            request.setAmount(new BigDecimal(payload));
            session.sendMessage(new TextMessage("Please enter your PIN:"));
            transactionSteps.put(sessionId, 4);
          }
          break;
        case 4:
          if (request.getTransactionType() == TransactionType.TRANSFER) {
            if (request.getTransferType() == TransferType.HELLOCASH) {
              request.setDestinationAccount(payload);
              AccountInfo nameEnquiryResponse = walletService.nameEnquiry(request.getDestinationAccount());
              session.sendMessage(new TextMessage("Account Name: " + nameEnquiryResponse.getAccountName()));
              session.sendMessage(new TextMessage("Please enter the amount:"));
              transactionSteps.put(sessionId, 5);
            } else if (request.getTransferType() == TransferType.OTHERS) {
              request.setDestinationAccount(payload);
              session.sendMessage(new TextMessage("Please enter the amount:"));
              transactionSteps.put(sessionId, 5);
            }
          } else {
            request.setPin(payload);
            TransactionsResponse transactionResponse = transactionService.performTransaction(request);
            sendTransactionResponse(session, transactionResponse);
            // Clean up session data after successful transaction
            transactionRequests.remove(sessionId);
            transactionSteps.remove(sessionId);
          }
          break;
        case 5:
          request.setAmount(new BigDecimal(payload));
          session.sendMessage(new TextMessage("Please enter your PIN:"));
          transactionSteps.put(sessionId, 6);
          break;
        case 6:
          request.setPin(payload);
          TransactionsResponse transactionResponse = transactionService.performTransaction(request);
          sendTransactionResponse(session, transactionResponse);
          // Clean up session data after successful transaction
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
          break;
        default:
          session.sendMessage(new TextMessage("Invalid transaction step. Please start over by selecting an option."));
          transactionRequests.remove(sessionId);
          transactionSteps.remove(sessionId);
      }
    } catch (Exception e) {
      log.error("Error processing transaction step: {}", e.getMessage());
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
      log.error("Unexpected error occurred: {}", e.getMessage(), e); // Log the exception for internal debugging
    }

    ErrorObject errorObject = new ErrorObject(statusCode, errorMessage);
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorObject)));
  }
}

