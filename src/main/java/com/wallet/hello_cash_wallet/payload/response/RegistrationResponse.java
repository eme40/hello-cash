package com.wallet.hello_cash_wallet.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistrationResponse {
  private String message;
  private String virtualAccountNumber;
  private String fullName;
  private BigDecimal balance;
}
