package com.wallet.hello_cash_wallet.exception;

public class AccountNotFoundException extends RuntimeException{
  private static final long serialVersionUID = 1;

  public AccountNotFoundException(String message) {
    super(message);
  }
}
