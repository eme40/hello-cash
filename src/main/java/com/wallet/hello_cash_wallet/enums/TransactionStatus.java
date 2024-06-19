package com.wallet.hello_cash_wallet.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {
  PENDING("Pending"),
  COMPLETED("Complete"),
  SUCCESS("Success"),
  FAILED("Failed");

  private final String label;

  TransactionStatus(String label) {
    this.label = label;
  }

}