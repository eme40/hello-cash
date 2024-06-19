package com.wallet.hello_cash_wallet.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
  DEPOSIT("Deposit"),
  WITHDRAW("Withdraw"),
  TRANSFER("Transfer"),
  BUY_CARD("Buy Card"),
  BUY_DATA("Buy Data"),
  CREDIT("Credit"),
  DEBIT("Debit");

  private final String label;

  TransactionType(String label) {
    this.label = label;
  }
}
