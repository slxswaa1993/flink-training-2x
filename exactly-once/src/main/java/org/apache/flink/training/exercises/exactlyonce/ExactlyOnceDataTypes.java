package org.apache.flink.training.exercises.exactlyonce;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Data types for the exactly-once semantics exercise.
 */
public class ExactlyOnceDataTypes {

    /**
     * Represents a financial transaction that requires exactly-once processing.
     *
     * <p>In financial systems, processing a transaction more than once
     * or missing a transaction can have serious consequences.
     */
    public static class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;

        public String transactionId;
        public String accountFrom;
        public String accountTo;
        public double amount;
        public String currency;
        public Instant timestamp;
        public String status;

        public Transaction() {
            this.transactionId = UUID.randomUUID().toString();
            this.timestamp = Instant.now();
            this.status = "PENDING";
        }

        public Transaction(String transactionId, String accountFrom, String accountTo,
                           double amount, String currency, Instant timestamp) {
            this.transactionId = transactionId;
            this.accountFrom = accountFrom;
            this.accountTo = accountTo;
            this.amount = amount;
            this.currency = currency;
            this.timestamp = timestamp;
            this.status = "PENDING";
        }

        public long getTimestampMillis() {
            return timestamp.toEpochMilli();
        }

        @Override
        public String toString() {
            return String.format(
                    "Transaction{id='%s', from='%s', to='%s', amount=%.2f %s, status='%s', time=%s}",
                    transactionId, accountFrom, accountTo, amount, currency, status, timestamp);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Transaction that = (Transaction) o;
            return Objects.equals(transactionId, that.transactionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(transactionId);
        }
    }

    /**
     * Represents the result of processing a transaction.
     */
    public static class TransactionResult implements Serializable {
        private static final long serialVersionUID = 1L;

        public String transactionId;
        public String status;  // COMPLETED, FAILED, DUPLICATE
        public double newBalanceFrom;
        public double newBalanceTo;
        public Instant processedAt;
        public String errorMessage;

        public TransactionResult() {}

        public TransactionResult(String transactionId, String status,
                                 double newBalanceFrom, double newBalanceTo) {
            this.transactionId = transactionId;
            this.status = status;
            this.newBalanceFrom = newBalanceFrom;
            this.newBalanceTo = newBalanceTo;
            this.processedAt = Instant.now();
        }

        public static TransactionResult success(String transactionId,
                                                double newBalanceFrom, double newBalanceTo) {
            return new TransactionResult(transactionId, "COMPLETED", newBalanceFrom, newBalanceTo);
        }

        public static TransactionResult duplicate(String transactionId) {
            TransactionResult result = new TransactionResult();
            result.transactionId = transactionId;
            result.status = "DUPLICATE";
            result.errorMessage = "Transaction already processed";
            result.processedAt = Instant.now();
            return result;
        }

        public static TransactionResult failed(String transactionId, String error) {
            TransactionResult result = new TransactionResult();
            result.transactionId = transactionId;
            result.status = "FAILED";
            result.errorMessage = error;
            result.processedAt = Instant.now();
            return result;
        }

        @Override
        public String toString() {
            if (status.equals("COMPLETED")) {
                return String.format(
                        "Result{txn='%s', status=%s, balFrom=%.2f, balTo=%.2f, at=%s}",
                        transactionId, status, newBalanceFrom, newBalanceTo, processedAt);
            } else {
                return String.format(
                        "Result{txn='%s', status=%s, error='%s', at=%s}",
                        transactionId, status, errorMessage, processedAt);
            }
        }
    }

    /**
     * Represents an account balance state.
     */
    public static class AccountBalance implements Serializable {
        private static final long serialVersionUID = 1L;

        public String accountId;
        public double balance;
        public long version;
        public Instant lastUpdated;

        public AccountBalance() {}

        public AccountBalance(String accountId, double initialBalance) {
            this.accountId = accountId;
            this.balance = initialBalance;
            this.version = 0;
            this.lastUpdated = Instant.now();
        }

        public void credit(double amount) {
            this.balance += amount;
            this.version++;
            this.lastUpdated = Instant.now();
        }

        public boolean debit(double amount) {
            if (this.balance >= amount) {
                this.balance -= amount;
                this.version++;
                this.lastUpdated = Instant.now();
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("Account{id='%s', balance=%.2f, v%d}",
                    accountId, balance, version);
        }
    }
}
