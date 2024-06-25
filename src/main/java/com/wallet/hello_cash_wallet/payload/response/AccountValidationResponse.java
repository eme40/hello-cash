package com.wallet.hello_cash_wallet.payload.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationResponse {
  private boolean status;
  private String message;
  private AccountData data;
}
