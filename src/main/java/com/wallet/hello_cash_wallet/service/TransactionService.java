package com.wallet.hello_cash_wallet.service;

import com.wallet.hello_cash_wallet.payload.request.CreditRequest;
import com.wallet.hello_cash_wallet.payload.request.TransactionRequest;
import com.wallet.hello_cash_wallet.payload.response.AccountInfo;
import com.wallet.hello_cash_wallet.payload.response.TransactionsResponse;

public interface TransactionService {
  TransactionsResponse performTransaction(TransactionRequest request) ;
}
