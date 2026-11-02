package com.prasad.paystream.core.service;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long accountId, long balanceMinor, long amountMinor) {
        super("Insufficient funds on account " + accountId
                + ": balance=" + balanceMinor + " requested=" + amountMinor);
    }
}
