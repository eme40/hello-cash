package com.wallet.hello_cash_wallet.repository;

import com.wallet.hello_cash_wallet.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEntityRepository extends JpaRepository<UserEntity,Long> {
  UserEntity findByPhoneNumber(String phoneNumber);

}
