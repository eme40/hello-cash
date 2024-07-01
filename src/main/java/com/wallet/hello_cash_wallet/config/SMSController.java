package com.wallet.hello_cash_wallet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/ws")
public class SMSController {

  private static final Logger logger = LoggerFactory.getLogger(SMSController.class);
  private final SMSHandlerService smsHandlerService;

  public SMSController(SMSHandlerService smsHandlerService) {
    this.smsHandlerService = smsHandlerService;
  }


  @PostMapping("/receive")
  public String receiveSMS(@RequestParam("From") String from, @RequestParam("To") String to, @RequestParam("Body") String body) {
    logger.info("Received SMS: From={}, To={}, Body={}", from, to, body);
    smsHandlerService.handleIncomingSms(to, body);
    return "Received: " + body;
  }
}

