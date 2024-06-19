package com.wallet.hello_cash_wallet.exception;


public class UserIdNotFoundException extends RuntimeException {
  public UserIdNotFoundException(String message) {
    super(message);
  }
}
