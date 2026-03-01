package org.apache.flink.training.exercises.exactlyonce;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.training.exercises.exactlyonce.ExactlyOnceDataTypes.*;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * Exactly-Once Pipeline Exercise (Phase 6.1)
 *
 * <p>This exercise teaches you how to implement end-to-end exactly-once
 * processing, which is critical for financial and other sensitive applications.
 *
 * <h2>Exercise Goals:</h2>
 * <ol>
 *   <li>Configure checkpointing for exactly-once semantics</li>
 *   <li>Implement idempotent processing with deduplication</li>
 *   <li>Maintain consistent state across failures</li>
 *   <li>Understand the two-phase commit protocol</li>
 * </ol>
 *
 * <h2>Key Concepts:</h2>
 * <ul>
 *   <li><b>Checkpointing:</b> Periodic snapshots of operator state</li>
 *   <li><b>Barriers:</b> Markers that flow through the stream to align checkpoints</li>
 *   <li><b>Two-Phase Commit:</b> Protocol for transactional sinks</li>
 *   <li><b>Idempotency:</b> Processing the same message multiple times yields same result</li>
 *   <li><b>Deduplication:</b> Detecting and filtering duplicate messages</li>
 * </ul>
 *
 * <h2>Exactly-Once Guarantees:</h2>
 * <ul>
 *   <li><b>At-most-once:</b> Messages may be lost, no duplicates</li>
 *   <li><b>At-least-once:</b> No message loss, may have duplicates</li>
 *   <li><b>Exactly-once:</b> No loss, no duplicates (requires special handling)</li>
 * </ul>
 *
 * <h2>Production Notes:</h2>
 * <p>For true end-to-end exactly-once:
 * <ul>
 *   <li>Source must be replayable (e.g., Kafka with committed offsets)</li>
 *   <li>Sink must support transactions (e.g., Kafka, JDBC with 2PC)</li>
 *   <li>Processing must be deterministic</li>
 * </ul>
 */
public class ExactlyOnceExercise {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // ================================================================
        // CRITICAL: Checkpoint Configuration for Exactly-Once
        // ================================================================

        // Enable checkpointing with exactly-once mode
        env.enableCheckpointing(10000, CheckpointingMode.EXACTLY_ONCE);

        CheckpointConfig checkpointConfig = env.getCheckpointConfig();

        // Minimum time between checkpoints (prevents checkpoint storms)
        checkpointConfig.setMinPauseBetweenCheckpoints(5000);

        // Checkpoint timeout - fail checkpoint if not complete in time
        checkpointConfig.setCheckpointTimeout(60000);

        // Maximum concurrent checkpoints
        checkpointConfig.setMaxConcurrentCheckpoints(1);

        // Enable externalized checkpoints for recovery after job cancellation
        // In Flink 2.x, checkpoints are retained by default
        // checkpointConfig.enableExternalizedCheckpoints() is deprecated

        // Tolerate checkpoint failures (useful for development)
        checkpointConfig.setTolerableCheckpointFailureNumber(3);

        // ================================================================
        // Create Transaction Source
        // ================================================================

        DataGeneratorSource<Transaction> source = TransactionGenerator.createSource(5.0);
        WatermarkStrategy<Transaction> watermarkStrategy =
                TransactionGenerator.createWatermarkStrategy(Duration.ofSeconds(5));

        DataStream<Transaction> transactions = env
                .fromSource(source, watermarkStrategy, "Transactions")
                .name("transaction-source");

        // ================================================================
        // Process Transactions with Exactly-Once Semantics
        // ================================================================

        DataStream<TransactionResult> results = transactions
                .keyBy(txn -> txn.accountFrom)  // Key by source account for consistent state
                .process(new ExactlyOnceTransactionProcessor())
                .name("transaction-processor");

        // Output results
        results.print("RESULT");

        // Filter and output only completed transactions
        results.filter(r -> r.status.equals("COMPLETED"))
                .print("COMPLETED");

        // Filter and output duplicates (for monitoring)
        results.filter(r -> r.status.equals("DUPLICATE"))
                .print("DUPLICATE");

        // Filter and output failures
        results.filter(r -> r.status.equals("FAILED"))
                .print("FAILED");

        env.execute("Exactly-Once Pipeline Exercise");
    }

    /**
     * Transaction processor that guarantees exactly-once processing.
     *
     * <p>Key features:
     * <ul>
     *   <li>Deduplication using transaction ID tracking</li>
     *   <li>Consistent balance updates with state</li>
     *   <li>Automatic state recovery from checkpoints</li>
     * </ul>
     */
    public static class ExactlyOnceTransactionProcessor
            extends KeyedProcessFunction<String, Transaction, TransactionResult> {

        // State: Account balance
        private ValueState<AccountBalance> balanceState;

        // State: Processed transaction IDs for deduplication
        // Maps transaction ID to processing timestamp
        private MapState<String, Long> processedTransactions;

        // How long to keep transaction IDs for deduplication (24 hours)
        private static final long DEDUP_RETENTION_MS = 24 * 60 * 60 * 1000L;

        @Override
        public void open(OpenContext openContext) throws Exception {
            balanceState = getRuntimeContext().getState(
                    new ValueStateDescriptor<>("balance", AccountBalance.class));

            processedTransactions = getRuntimeContext().getMapState(
                    new MapStateDescriptor<>("processed-txns", String.class, Long.class));
        }

        @Override
        public void processElement(Transaction txn, Context ctx, Collector<TransactionResult> out)
                throws Exception {

            // ============================================================
            // Step 1: Check for duplicate (idempotency check)
            // ============================================================
            Long previousProcessingTime = processedTransactions.get(txn.transactionId);
            if (previousProcessingTime != null) {
                // Already processed this transaction - emit duplicate result
                out.collect(TransactionResult.duplicate(txn.transactionId));
                return;
            }

            // ============================================================
            // Step 2: Get or initialize account balance
            // ============================================================
            AccountBalance balance = balanceState.value();
            if (balance == null) {
                // Initialize with a starting balance (in production, load from DB)
                balance = new AccountBalance(txn.accountFrom, 10000.0);
            }

            // ============================================================
            // Step 3: Process the transaction
            // ============================================================
            boolean success = balance.debit(txn.amount);

            if (success) {
                // Update state
                balanceState.update(balance);

                // Mark transaction as processed
                processedTransactions.put(txn.transactionId, ctx.timestamp());

                // Register timer to clean up old transaction IDs
                ctx.timerService().registerEventTimeTimer(
                        ctx.timestamp() + DEDUP_RETENTION_MS);

                // Emit success result
                // Note: In a real system, we'd also need to credit the target account
                // This would require a more complex two-phase approach
                out.collect(TransactionResult.success(
                        txn.transactionId,
                        balance.balance,
                        0.0  // Target account balance not tracked in this simplified example
                ));
            } else {
                // Insufficient funds
                out.collect(TransactionResult.failed(
                        txn.transactionId,
                        String.format("Insufficient funds: needed %.2f, available %.2f",
                                txn.amount, balance.balance)));
            }
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<TransactionResult> out)
                throws Exception {
            // Clean up old transaction IDs to prevent state from growing unbounded
            long cutoff = timestamp - DEDUP_RETENTION_MS;

            // Note: In production, use a more efficient data structure
            // This is simplified for educational purposes
            for (String txnId : processedTransactions.keys()) {
                Long processedTime = processedTransactions.get(txnId);
                if (processedTime != null && processedTime < cutoff) {
                    processedTransactions.remove(txnId);
                }
            }
        }
    }
}
