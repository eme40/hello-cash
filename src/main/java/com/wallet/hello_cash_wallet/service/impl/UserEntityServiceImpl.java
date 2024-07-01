package com.wallet.hello_cash_wallet.service.impl;

import com.wallet.hello_cash_wallet.entities.UserEntity;
import com.wallet.hello_cash_wallet.entities.Wallet;
import com.wallet.hello_cash_wallet.payload.request.RegistrationRequest;
import com.wallet.hello_cash_wallet.payload.response.RegistrationResponse;
import com.wallet.hello_cash_wallet.repository.UserEntityRepository;
import com.wallet.hello_cash_wallet.repository.WalletRepository;
import com.wallet.hello_cash_wallet.service.TwilioService;
import com.wallet.hello_cash_wallet.service.UserEntityService;
import com.wallet.hello_cash_wallet.util.AccountNumberGenerator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Slf4j
@Service
@AllArgsConstructor
public class UserEntityServiceImpl implements UserEntityService {
  private final UserEntityRepository userRepository;
  private final WalletRepository walletRepository;
  private final TwilioService twilioService;


  @Override
  public RegistrationResponse createUser(RegistrationRequest request) {
    try {
      UserEntity newUser = UserEntity.builder()
              .fullName(request.getFullName())
              .dateOfBirth(request.getDateOfBirth())
              .bvn(request.getBvn())
              .phoneNumber(request.getPhoneNumber())
              .build();
      newUser.setPin(request.getPin());

      userRepository.save(newUser);

      Wallet newWallet = Wallet.builder()
              .user(newUser)
              .accountName(newUser.getFullName())
              .balance(BigDecimal.ZERO) // Initial balance
              .virtualAccountNumber(AccountNumberGenerator.generateAccountNumber())
              .build();

      walletRepository.save(newWallet);

      String message = "Welcome " + newWallet.getAccountName() + "! Your account has been created successfully. " +
              "Account Number: " + newWallet.getVirtualAccountNumber();
      twilioService.sendSms(request.getPhoneNumber(), message);

      return RegistrationResponse.builder()
              .balance(newWallet.getBalance())
              .fullName(newWallet.getAccountName())
              .virtualAccountNumber(newWallet.getVirtualAccountNumber())
              .build();
    } catch (Exception e) {
      log.error("Error creating user: {}", e.getMessage());
      twilioService.sendSms(request.getPhoneNumber(), "Error creating user. Please try again later.");
      throw new RuntimeException("Error creating user", e);
    }
  }



  public String getAccountNumberByPhoneNumber(String phoneNumber) {
    UserEntity user = userRepository.findByPhoneNumber(phoneNumber);
    if (user == null) {
      throw new IllegalArgumentException("No user found with the given phone number");
    }
    return user.getAccount().getVirtualAccountNumber();
  }
}
