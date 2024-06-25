package com.wallet.hello_cash_wallet.payload.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wallet.hello_cash_wallet.enums.TransactionType;
import com.wallet.hello_cash_wallet.enums.TransferType;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder
public class TransactionRequest {
  private Long id;
  private String virtualAccountNumber;
  private String destinationAccount;
  private String destinationAccountName;
  private BigDecimal amount;
  private TransactionType transactionType;
  private String pin;
  private TransferType transferType;
  private String bankCode;
  private String phoneNumber;
}
