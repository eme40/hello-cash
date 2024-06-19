package com.wallet.hello_cash_wallet.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AccountInfo {
  private String accountName;
  private BigDecimal balance;
  private String virtualAccountNumber;
}