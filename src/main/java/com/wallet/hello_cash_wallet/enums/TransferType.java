package com.wallet.hello_cash_wallet.enums;

import lombok.Getter;

@Getter
public enum TransferType {
    HELLOCASH("HelloCash"),
    OTHERS("Others");

    private final String label;

    TransferType(String label) {
        this.label = label;
    }
}
