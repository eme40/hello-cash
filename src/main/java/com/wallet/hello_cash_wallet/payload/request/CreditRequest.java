package com.wallet.hello_cash_wallet.payload.request;

import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CreditRequest {
  private String virtualAccountNumber;
  private BigDecimal amount;
}

