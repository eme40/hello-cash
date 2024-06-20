package com.wallet.hello_cash_wallet.service;

import com.wallet.hello_cash_wallet.payload.response.AccountInfo;

public interface WalletService {
    AccountInfo nameEnquiry (String virtualAccountNumber);
}
