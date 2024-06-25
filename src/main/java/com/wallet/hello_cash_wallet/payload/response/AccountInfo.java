package com.wallet.hello_cash_wallet.payload.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountInfo {
  private String accountName;
  private String accountNumber;
  private String bankCode;

//  public AccountInfo(String accountName, String accountNumber) {
//    this.accountName = accountName;
//    this.accountNumber = accountNumber;
//  }
}