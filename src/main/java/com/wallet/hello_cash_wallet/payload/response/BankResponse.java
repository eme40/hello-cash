package com.wallet.hello_cash_wallet.payload.response;

import com.wallet.hello_cash_wallet.payload.request.Bank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BankResponse {
  private boolean status;
  private List<Bank> data;

}
