package com.wallet.hello_cash_wallet.exception;


import lombok.Data;

import java.util.Date;

@Data
public class ErrorObject {
  private int statusCode;
  private String message;
  private Date timestamp;

  public ErrorObject(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
    this.timestamp = new Date();
  }
}

