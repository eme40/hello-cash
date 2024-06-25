package com.wallet.hello_cash_wallet.service;

import com.wallet.hello_cash_wallet.payload.request.Bank;
import com.wallet.hello_cash_wallet.payload.response.AccountInfo;
import com.wallet.hello_cash_wallet.payload.response.AccountValidationResponse;

import java.util.List;

public interface PayStackService {
  boolean isValidBankCode(String bankCode);
  List<Bank> getBanks();
  AccountInfo validateAccount(String accountNumber, String bankCode);
}
