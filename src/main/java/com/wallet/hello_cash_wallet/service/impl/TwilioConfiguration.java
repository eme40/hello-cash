package com.wallet.hello_cash_wallet.service.impl;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class TwilioConfiguration {

  @Value("${twilio.account_sid}")
  private String accountSid;

  @Value("${twilio.auth_token}")
  private String authToken;

  @Value("${twilio.phone_number}")
  private String fromNumber;

  @PostConstruct
  public void initTwilio() {
    Twilio.init(accountSid, authToken);
  }

}

