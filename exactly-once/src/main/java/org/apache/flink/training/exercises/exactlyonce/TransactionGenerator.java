package org.apache.flink.training.exercises.exactlyonce;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.training.exercises.exactlyonce.ExactlyOnceDataTypes.Transaction;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * Generator for financial transactions.
 */
public class TransactionGenerator {

    private static final String[] ACCOUNTS = {
            "ACC001", "ACC002", "ACC003", "ACC004", "ACC005",
            "ACC006", "ACC007", "ACC008", "ACC009", "ACC010"
    };

    private static final String[] CURRENCIES = {"USD", "EUR", "GBP"};

    public static DataGeneratorSource<Transaction> createSource(double transactionsPerSecond) {
        return new DataGeneratorSource<>(
                new TransactionGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(transactionsPerSecond),
                TypeInformation.of(Transaction.class));
    }

    public static WatermarkStrategy<Transaction> createWatermarkStrategy(Duration maxOutOfOrderness) {
        return WatermarkStrategy
                .<Transaction>forBoundedOutOfOrderness(maxOutOfOrderness)
                .withTimestampAssigner((txn, timestamp) -> txn.getTimestampMillis());
    }

    private static class TransactionGeneratorFunction implements GeneratorFunction<Long, Transaction> {
        private static final long serialVersionUID = 1L;

        private transient Random random;
        private transient long baseTime;

        @Override
        public Transaction map(Long sequence) throws Exception {
            if (random == null) {
                random = new Random(sequence);
                baseTime = System.currentTimeMillis();
            }

            String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

            // Select two different accounts
            int fromIndex = random.nextInt(ACCOUNTS.length);
            int toIndex = (fromIndex + 1 + random.nextInt(ACCOUNTS.length - 1)) % ACCOUNTS.length;

            String accountFrom = ACCOUNTS[fromIndex];
            String accountTo = ACCOUNTS[toIndex];

            // Generate amount (mostly small, occasionally large)
            double amount;
            if (random.nextDouble() < 0.9) {
                amount = 10 + random.nextDouble() * 990;  // $10 - $1000
            } else {
                amount = 1000 + random.nextDouble() * 9000;  // $1000 - $10000
            }
            amount = Math.round(amount * 100.0) / 100.0;  // Round to cents

            String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];

            // Occasionally generate duplicate transaction IDs to test idempotency
            if (random.nextDouble() < 0.02 && sequence > 10) {
                transactionId = "TXN-DUPLICATE-" + (sequence - random.nextInt(10));
            }

            long timeOffset = sequence * 100 + random.nextInt(200);
            Instant timestamp = Instant.ofEpochMilli(baseTime + timeOffset);

            return new Transaction(transactionId, accountFrom, accountTo, amount, currency, timestamp);
        }
    }
}
