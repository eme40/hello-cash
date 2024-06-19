package com.wallet.hello_cash_wallet.service;

import com.wallet.hello_cash_wallet.payload.request.RegistrationRequest;
import com.wallet.hello_cash_wallet.payload.response.RegistrationResponse;

public interface UserEntityService {
  RegistrationResponse createUser(RegistrationRequest request);
}
