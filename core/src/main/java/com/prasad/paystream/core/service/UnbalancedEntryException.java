package com.prasad.paystream.core.service;

/** Thrown when a journal entry's total debits do not equal its total credits. Nothing is persisted. */
public class UnbalancedEntryException extends RuntimeException {

    public UnbalancedEntryException(long totalDebits, long totalCredits) {
        super("Journal entry is not balanced: debits=" + totalDebits + " credits=" + totalCredits);
    }
}
