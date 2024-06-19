package com.wallet.hello_cash_wallet.util;

public class AccountNumberGenerator {
  public static String generateAccountNumber() {
    String prefix = "244";
    int min = 1000000;
    int max = 9999999;

    int randNumber = (int) Math.floor(Math.random() * (max - min + 1) + min);

    String randomDigits = String.valueOf(randNumber);

    StringBuilder accountNumber = new StringBuilder();
    return accountNumber.append(prefix).append(randomDigits).toString();
  }
}
