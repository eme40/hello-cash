package com.wallet.hello_cash_wallet.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "wallet")
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Wallet extends BaseClass{
  @NotBlank
  @Column(unique = true)
  private String virtualAccountNumber;
  @NotBlank
  private String accountName;
  @NotNull
  private BigDecimal balance;
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;
  @OneToMany(mappedBy = "virtualAccountNumber", cascade = CascadeType.ALL,orphanRemoval = true)
  private List<Transaction> transactions;

  public void credit(BigDecimal amount){
    this.balance = amount.add(balance);
  }
  public void debit(BigDecimal amount){
    this.balance = balance.subtract(amount);
  }
}
