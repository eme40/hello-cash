package com.wallet.hello_cash_wallet.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.hello_cash_wallet.payload.request.Bank;
import com.wallet.hello_cash_wallet.payload.response.AccountInfo;
import com.wallet.hello_cash_wallet.payload.response.BankResponse;
import com.wallet.hello_cash_wallet.service.PayStackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.List;

@Service
@Slf4j
public class PayStackServiceImpl implements PayStackService {

  @Value("${spring.paystack.secret.key}")
  private String payStackApiKey;

  @Value("${spring.paystack.api.url}")
  private String payStackApiUrl;

  private final RestTemplate restTemplate;

  public PayStackServiceImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public AccountInfo validateAccount(String accountNumber, String bankCode) {
    String url = payStackApiUrl + "/bank/resolve?account_number=" + accountNumber + "&bank_code=" + bankCode;
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + payStackApiKey);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
      log.info("PayStack API response: {}", response.getBody());

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(response.getBody());

      if (root.path("status").asBoolean() && root.has("data")) {
        JsonNode data = root.path("data");
        String accountName = data.path("account_name").asText(null);
        String accountNumberResponse = data.path("account_number").asText(null);
        String bankCodeResponse = bankCode;

        if (accountName != null && accountNumberResponse != null) {
          AccountInfo accountInfo = new AccountInfo();
          accountInfo.setAccountName(accountName);
          accountInfo.setAccountNumber(accountNumberResponse);
          accountInfo.setBankCode(bankCodeResponse);
          return accountInfo;
        } else {
          log.error("Account name or number is null in the response from PayStack: {}", response.getBody());
          throw new RuntimeException("Invalid account details in response from PayStack");
        }
      } else {
        log.error("Invalid response from PayStack: {}", response.getBody());
        throw new RuntimeException("Invalid response from PayStack");
      }
    } catch (Exception e) {
      log.error("Error validating account: {}", e.getMessage(), e);
      throw new RuntimeException("Error validating account", e);
    }
  }

  @Override
  public List<Bank> getBanks() {
    String url = payStackApiUrl + "/bank";
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + payStackApiKey);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<BankResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, BankResponse.class);
      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        return response.getBody().getData();
      } else {
        log.error("Failed to retrieve banks: {}", response.getBody());
        throw new RuntimeException("Failed to retrieve banks");
      }
    } catch (Exception e) {
      log.error("Error retrieving banks: {}", e.getMessage(), e);
      throw new RuntimeException("Error retrieving banks", e);
    }
  }

  @Override
  public boolean isValidBankCode(String bankCode) {
    List<Bank> banks = getBanks();
    return banks.stream().anyMatch(bank -> bank.getCode().equals(bankCode));
  }
}



