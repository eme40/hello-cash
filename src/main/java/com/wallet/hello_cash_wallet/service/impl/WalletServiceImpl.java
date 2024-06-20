package com.wallet.hello_cash_wallet.service.impl;

import com.wallet.hello_cash_wallet.entities.Wallet;
import com.wallet.hello_cash_wallet.exception.AccountNotFoundException;
import com.wallet.hello_cash_wallet.payload.response.AccountInfo;
import com.wallet.hello_cash_wallet.repository.WalletRepository;
import com.wallet.hello_cash_wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    @Override
    public AccountInfo nameEnquiry(String virtualAccountNumber) {
        boolean isAccountExists = walletRepository.existsByVirtualAccountNumber(virtualAccountNumber);
        if (!isAccountExists) {
            throw new AccountNotFoundException("Account not found!");
        }
        Wallet foundUser = walletRepository.findByVirtualAccountNumber(virtualAccountNumber);

        return AccountInfo.builder()
                .accountName(foundUser.getAccountName())
                .build();
    }
}
