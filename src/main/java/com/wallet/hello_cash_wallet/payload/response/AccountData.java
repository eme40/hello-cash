package com.wallet.hello_cash_wallet.payload.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountData {
  private String accountNumber;
  private String accountName;
  private String bankCode;
}
