package com.wallet.hello_cash_wallet.entities;

import com.wallet.hello_cash_wallet.enums.TransactionStatus;
import com.wallet.hello_cash_wallet.enums.TransactionType;
import com.wallet.hello_cash_wallet.enums.TransferType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transactions")
@Entity
public class Transaction extends BaseClass {
  @Enumerated(EnumType.STRING)
  private TransactionType transactionType;
  private BigDecimal amount;
  private String description;
  @Enumerated(EnumType.STRING)
  private TransactionStatus transactionStatus;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id")
  private Wallet virtualAccountNumber;
  @Enumerated(EnumType.STRING)
  private TransferType transferType;
}
