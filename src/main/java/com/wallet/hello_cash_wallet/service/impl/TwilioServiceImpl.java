package com.wallet.hello_cash_wallet.service.impl;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.wallet.hello_cash_wallet.service.TwilioService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioServiceImpl implements TwilioService {
  private final TwilioConfiguration twilioConfiguration;
  private static final Logger logger = LoggerFactory.getLogger(TwilioServiceImpl.class);


  @Autowired
  public TwilioServiceImpl(TwilioConfiguration twilioConfiguration) {
    this.twilioConfiguration = twilioConfiguration;
    Twilio.init(
            twilioConfiguration.getAccountSid(),
            twilioConfiguration.getAuthToken()
    );
  }

  public void sendSms(String to, String message) {
    try {
      String formattedTo = formatPhoneNumber(to);
      Message.creator(
              new PhoneNumber(formattedTo),
              new PhoneNumber(twilioConfiguration.getFromNumber()),
              message
      ).create();
    } catch (ApiException e) {
      log.error("Failed to send SMS to {} with Twilio: {}", to, e.getMessage());
      log.error("Exception details: ", e);
    }
  }

  private String formatPhoneNumber(String phoneNumber) {
    // Remove any non-digit characters
    phoneNumber = phoneNumber.replaceAll("\\D", "");

    if (phoneNumber.startsWith("234")) {
      // If the phone number already starts with the country code, use it directly
      return "+" + phoneNumber;
    } else if (phoneNumber.startsWith("0")) {
      // If the phone number starts with '0', remove the '0' and add the country code
      return "+234" + phoneNumber.substring(1);
    } else {
      // Otherwise, assume it's missing the country code and add it
      return "+234" + phoneNumber;
    }
  }

}


