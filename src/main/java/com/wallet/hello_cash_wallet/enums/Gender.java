package com.wallet.hello_cash_wallet.enums;


import lombok.Getter;

@Getter

public enum Gender {
  FEMALE("female"),
  MALE("male");
  private final String label;

  Gender(String label) {
    this.label = label;
  }
}
