package com.wallet.hello_cash_wallet.service.impl;

import com.wallet.hello_cash_wallet.entities.UserEntity;
import com.wallet.hello_cash_wallet.entities.Wallet;
import com.wallet.hello_cash_wallet.payload.request.RegistrationRequest;
import com.wallet.hello_cash_wallet.payload.response.RegistrationResponse;
import com.wallet.hello_cash_wallet.repository.UserEntityRepository;
import com.wallet.hello_cash_wallet.repository.WalletRepository;
import com.wallet.hello_cash_wallet.service.UserEntityService;
import com.wallet.hello_cash_wallet.util.AccountNumberGenerator;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class UserEntityServiceImpl implements UserEntityService {
  private final UserEntityRepository userRepository;
  private final WalletRepository walletRepository;

  @Override
  public RegistrationResponse createUser(RegistrationRequest request) {
    UserEntity newUser = UserEntity.builder()
            .fullName(request.getFullName())
            .dateOfBirth(request.getDateOfBirth())
            .bvn(request.getBvn())
            .pin(request.getPin())
            .build();
    userRepository.save(newUser);
    Wallet newWallet = Wallet.builder()
            .user(newUser)
            .accountName(newUser.getFullName())
            .balance(BigDecimal.valueOf(0.00))
            .virtualAccountNumber(AccountNumberGenerator.generateAccountNumber())
            .build();
    walletRepository.save(newWallet);

    return RegistrationResponse.builder()
            .balance(newWallet.getBalance())
            .fullName(newWallet.getAccountName())
            .virtualAccountNumber(newWallet.getVirtualAccountNumber())
            .build();
  }
}
