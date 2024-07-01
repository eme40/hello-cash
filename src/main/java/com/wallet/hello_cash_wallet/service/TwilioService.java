package com.wallet.hello_cash_wallet.service;

public interface TwilioService {
  void sendSms(String toPhoneNumber, String message);
}
