package org.apache.flink.training.exercises.cdcpipeline;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * Generator for simulated CDC events.
 *
 * <p>Simulates database change events as they would be captured by
 * Debezium or Flink CDC connectors from a MySQL/PostgreSQL database.
 */
public class ChangeEventGenerator {

    private static final String[] TABLES = {"customers", "orders", "products", "inventory"};

    public static DataGeneratorSource<ChangeEvent> createSource(double eventsPerSecond) {
        GeneratorFunction<Long, ChangeEvent> generatorFunction = new ChangeEventGeneratorFunction();

        return new DataGeneratorSource<>(
                generatorFunction,
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(eventsPerSecond),
                TypeInformation.of(ChangeEvent.class));
    }

    public static WatermarkStrategy<ChangeEvent> createWatermarkStrategy(Duration maxOutOfOrderness) {
        return WatermarkStrategy
                .<ChangeEvent>forBoundedOutOfOrderness(maxOutOfOrderness)
                .withTimestampAssigner((event, timestamp) -> event.getTimestampMillis());
    }

    private static class ChangeEventGeneratorFunction implements GeneratorFunction<Long, ChangeEvent> {
        private static final long serialVersionUID = 1L;

        private transient Random random;
        private transient long baseTime;
        private transient long transactionId;

        @Override
        public ChangeEvent map(Long sequence) throws Exception {
            if (random == null) {
                random = new Random(sequence);
                baseTime = System.currentTimeMillis();
                transactionId = 1000;
            }

            String tableName = TABLES[random.nextInt(TABLES.length)];

            // Generate operation type with realistic distribution
            ChangeEvent.Operation operation = generateOperation();

            String primaryKey = generatePrimaryKey(tableName, sequence);
            String beforeData = null;
            String afterData = null;

            // Generate before/after data based on operation type
            switch (operation) {
                case INSERT:
                case READ:
                    afterData = generateRecordData(tableName, primaryKey);
                    break;
                case UPDATE:
                    beforeData = generateRecordData(tableName, primaryKey);
                    afterData = generateUpdatedRecordData(tableName, primaryKey);
                    break;
                case DELETE:
                    beforeData = generateRecordData(tableName, primaryKey);
                    break;
            }

            // Simulate transaction batching (multiple events per transaction)
            if (random.nextDouble() < 0.3) {
                transactionId++;
            }

            long timeOffset = sequence * 50 + random.nextInt(100);
            Instant timestamp = Instant.ofEpochMilli(baseTime + timeOffset);

            return new ChangeEvent(
                    operation,
                    tableName,
                    primaryKey,
                    beforeData,
                    afterData,
                    timestamp,
                    transactionId,
                    sequence);
        }

        private ChangeEvent.Operation generateOperation() {
            double rand = random.nextDouble();
            if (rand < 0.40) {
                return ChangeEvent.Operation.INSERT;
            } else if (rand < 0.85) {
                return ChangeEvent.Operation.UPDATE;
            } else {
                return ChangeEvent.Operation.DELETE;
            }
        }

        private String generatePrimaryKey(String tableName, long sequence) {
            int id = (int) (sequence % 1000) + 1;
            switch (tableName) {
                case "customers":
                    return "CUST_" + String.format("%05d", id);
                case "orders":
                    return "ORD_" + String.format("%08d", sequence);
                case "products":
                    return "PROD_" + String.format("%04d", id % 100);
                case "inventory":
                    return "INV_" + String.format("%04d", id % 100);
                default:
                    return "ID_" + id;
            }
        }

        private String generateRecordData(String tableName, String primaryKey) {
            switch (tableName) {
                case "customers":
                    return String.format(
                            "{\"id\":\"%s\",\"name\":\"Customer %s\",\"email\":\"customer%s@example.com\",\"status\":\"active\"}",
                            primaryKey, primaryKey.substring(5), primaryKey.substring(5));
                case "orders":
                    return String.format(
                            "{\"id\":\"%s\",\"customer_id\":\"CUST_%05d\",\"total\":%.2f,\"status\":\"pending\"}",
                            primaryKey, random.nextInt(1000), random.nextDouble() * 1000);
                case "products":
                    return String.format(
                            "{\"id\":\"%s\",\"name\":\"Product %s\",\"price\":%.2f,\"category\":\"category_%d\"}",
                            primaryKey, primaryKey.substring(5), random.nextDouble() * 500, random.nextInt(10));
                case "inventory":
                    return String.format(
                            "{\"product_id\":\"%s\",\"warehouse\":\"WH_%d\",\"quantity\":%d}",
                            primaryKey, random.nextInt(5), random.nextInt(1000));
                default:
                    return "{}";
            }
        }

        private String generateUpdatedRecordData(String tableName, String primaryKey) {
            // Generate slightly modified data for updates
            switch (tableName) {
                case "customers":
                    String status = random.nextBoolean() ? "active" : "inactive";
                    return String.format(
                            "{\"id\":\"%s\",\"name\":\"Customer %s\",\"email\":\"customer%s@example.com\",\"status\":\"%s\"}",
                            primaryKey, primaryKey.substring(5), primaryKey.substring(5), status);
                case "orders":
                    String orderStatus = random.nextBoolean() ? "confirmed" : "shipped";
                    return String.format(
                            "{\"id\":\"%s\",\"customer_id\":\"CUST_%05d\",\"total\":%.2f,\"status\":\"%s\"}",
                            primaryKey, random.nextInt(1000), random.nextDouble() * 1000, orderStatus);
                case "products":
                    return String.format(
                            "{\"id\":\"%s\",\"name\":\"Product %s\",\"price\":%.2f,\"category\":\"category_%d\"}",
                            primaryKey, primaryKey.substring(5), random.nextDouble() * 500, random.nextInt(10));
                case "inventory":
                    return String.format(
                            "{\"product_id\":\"%s\",\"warehouse\":\"WH_%d\",\"quantity\":%d}",
                            primaryKey, random.nextInt(5), random.nextInt(1000));
                default:
                    return "{}";
            }
        }
    }
}
