package com.wallet.hello_cash_wallet.service.impl;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BankService {
  private final Map<String, String> bankNameToCodeMap;

  public BankService() {
    bankNameToCodeMap = new HashMap<>();
    bankNameToCodeMap.put("GTBank", "058");
    bankNameToCodeMap.put("FirstBank", "011");
    bankNameToCodeMap.put("ZenithBank", "057");
    bankNameToCodeMap.put("FidelityBank", "070");

   }

  public String getBankCode(String bankName) {
    return bankNameToCodeMap.get(bankName);
  }

  public boolean isValidBankName(String bankName) {
    return bankNameToCodeMap.containsKey(bankName);
  }

  public List<String> getBankNames() {
    return new ArrayList<>(bankNameToCodeMap.keySet());
  }
}
