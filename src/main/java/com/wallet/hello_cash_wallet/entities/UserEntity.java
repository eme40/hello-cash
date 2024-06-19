package com.wallet.hello_cash_wallet.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wallet.hello_cash_wallet.enums.Gender;
import com.wallet.hello_cash_wallet.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@Entity
@Table(name = "users")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)

public class UserEntity extends BaseClass{
  private String fullName;
  @Column(unique = true)
  private String bvn;
  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Wallet account;
  private LocalDate dateOfBirth;
  private  String pin;
  private String address;
  private Gender gender;
  private Role role;

  public UserEntity() {
    this.address = "16B ";
  }

}
