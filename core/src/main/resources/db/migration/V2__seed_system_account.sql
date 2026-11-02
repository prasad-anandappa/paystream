-- The system cash account is the platform's funding source. It is the one
-- account allowed to go negative (allow_negative = TRUE): deposits into user
-- accounts are ordinary double-entry transfers out of SYSTEM_CASH, so the
-- ledger balances from the very first transaction.
INSERT INTO accounts (name, type, currency, balance_minor, allow_negative, version, created_at)
VALUES ('SYSTEM_CASH', 'ASSET', 'USD', 0, TRUE, 0, CURRENT_TIMESTAMP);
