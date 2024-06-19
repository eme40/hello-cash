package com.wallet.hello_cash_wallet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.hello_cash_wallet.enums.TransactionType;
import com.wallet.hello_cash_wallet.exception.AccountNotFoundException;
import com.wallet.hello_cash_wallet.exception.ErrorObject;
import com.wallet.hello_cash_wallet.exception.UserIdNotFoundException;
import com.wallet.hello_cash_wallet.payload.request.RegistrationRequest;
import com.wallet.hello_cash_wallet.payload.request.TransactionRequest;
import com.wallet.hello_cash_wallet.payload.response.RegistrationResponse;
import com.wallet.hello_cash_wallet.payload.response.TransactionsResponse;
import com.wallet.hello_cash_wallet.service.TransactionService;
import com.wallet.hello_cash_wallet.service.UserEntityService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

  private final TransactionService transactionService;
  private final UserEntityService userEntityService;

  // Maps to keep track of registration and transaction state for each session
  private Map<String, RegistrationRequest> registrationRequests = new HashMap<>();
  private Map<String, Integer> registrationSteps = new HashMap<>();

  private Map<String, TransactionRequest> transactionRequests = new HashMap<>();
  private Map<String, Integer> transactionSteps = new HashMap<>();

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
          session.sendMessage(new TextMessage("Please enter your BVN:"));
          registrationSteps.put(sessionId, 3);
          break;
        case 3:
          request.setBvn(payload);
          session.sendMessage(new TextMessage("Please enter your PIN:"));
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
          try {
            request.setTransactionType(TransactionType.valueOf(payload.toUpperCase()));
          } catch (IllegalArgumentException e) {
            session.sendMessage(new TextMessage("Invalid transaction type. Please enter 'credit', 'debit', or 'transfer'."));
            return;
          }
          session.sendMessage(new TextMessage("Please enter the amount:"));
          transactionSteps.put(sessionId, 2);
          break;
        case 2:
          request.setAmount(new BigDecimal(payload));
          session.sendMessage(new TextMessage("Please enter your account number:"));
          transactionSteps.put(sessionId, 3);
          break;
        case 3:
          request.setVirtualAccountNumber(payload);
          if (request.getTransactionType() == TransactionType.TRANSFER) {
            session.sendMessage(new TextMessage("Please enter the destination account number:"));
            transactionSteps.put(sessionId, 4);
          } else {
            session.sendMessage(new TextMessage("Please enter your PIN:"));
            transactionSteps.put(sessionId, 5);
          }
          break;
        case 4:
          request.setDestinationAccount(payload);
          session.sendMessage(new TextMessage("Please enter your PIN:"));
          transactionSteps.put(sessionId, 5);
          break;
        case 5:
          request.setPin(payload);
          TransactionsResponse transactionResponse = transactionService.performTransaction(request);

          String response = "Transaction Response:\n" +
                  "Response Code: " + transactionResponse.getStatusCode() + "\n" +
                  "Response Message: " + transactionResponse.getMessage();
          session.sendMessage(new TextMessage(response));

          // Clean up session data after transaction
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

