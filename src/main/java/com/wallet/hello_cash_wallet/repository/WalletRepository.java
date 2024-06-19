package com.wallet.hello_cash_wallet.repository;

import com.wallet.hello_cash_wallet.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet,Long> {
  boolean existsByVirtualAccountNumber(String account);
  Wallet findByVirtualAccountNumber(String account);
}
