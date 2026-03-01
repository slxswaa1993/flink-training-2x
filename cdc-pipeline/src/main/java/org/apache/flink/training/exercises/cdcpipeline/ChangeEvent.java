package org.apache.flink.training.exercises.cdcpipeline;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a database change event (CDC event).
 *
 * <p>This simulates what you would receive from a CDC connector like
 * Debezium or Flink CDC when capturing changes from a database.
 */
public class ChangeEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Operation {
        INSERT,   // 'c' in Debezium
        UPDATE,   // 'u' in Debezium
        DELETE,   // 'd' in Debezium
        READ      // 'r' in Debezium (snapshot)
    }

    public Operation operation;
    public String tableName;
    public String primaryKey;
    public String beforeData;  // JSON string of before state (for UPDATE/DELETE)
    public String afterData;   // JSON string of after state (for INSERT/UPDATE)
    public Instant timestamp;
    public long transactionId;
    public long position;  // Binlog position or LSN

    public ChangeEvent() {
        this.timestamp = Instant.now();
    }

    public ChangeEvent(
            Operation operation,
            String tableName,
            String primaryKey,
            String beforeData,
            String afterData,
            Instant timestamp,
            long transactionId,
            long position) {
        this.operation = operation;
        this.tableName = tableName;
        this.primaryKey = primaryKey;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.timestamp = timestamp;
        this.transactionId = transactionId;
        this.position = position;
    }

    public long getTimestampMillis() {
        return timestamp.toEpochMilli();
    }

    public boolean isInsert() {
        return operation == Operation.INSERT || operation == Operation.READ;
    }

    public boolean isUpdate() {
        return operation == Operation.UPDATE;
    }

    public boolean isDelete() {
        return operation == Operation.DELETE;
    }

    @Override
    public String toString() {
        return String.format(
                "CDC{op=%s, table='%s', key='%s', txn=%d, pos=%d, time=%s}",
                operation, tableName, primaryKey, transactionId, position, timestamp);
    }
}
