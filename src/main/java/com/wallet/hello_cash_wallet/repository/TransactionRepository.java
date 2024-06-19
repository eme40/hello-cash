package com.wallet.hello_cash_wallet.repository;

import com.wallet.hello_cash_wallet.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
}
