package org.apache.flink.training.exercises.sqldashboard;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * Generator for Order data used in SQL Dashboard exercises.
 */
public class OrderGenerator {

    private static final String[] CUSTOMERS = {
            "C001", "C002", "C003", "C004", "C005", "C006", "C007", "C008", "C009", "C010"
    };

    private static final String[] REGIONS = {
            "North America", "Europe", "Asia Pacific", "Latin America", "Middle East"
    };

    private static final String[] CATEGORIES = {
            "Electronics", "Clothing", "Home & Garden", "Sports", "Books"
    };

    private static final String[][] PRODUCTS = {
            {"ELEC001", "Laptop", "1299.99"},
            {"ELEC002", "Smartphone", "799.99"},
            {"ELEC003", "Headphones", "199.99"},
            {"CLTH001", "T-Shirt", "29.99"},
            {"CLTH002", "Jeans", "79.99"},
            {"HOME001", "Lamp", "49.99"},
            {"HOME002", "Chair", "149.99"},
            {"SPRT001", "Running Shoes", "119.99"},
            {"BOOK001", "Programming Book", "59.99"},
            {"BOOK002", "Novel", "19.99"}
    };

    private static final String[] STATUSES = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED"};

    public static DataGeneratorSource<Order> createSource(double ordersPerSecond) {
        GeneratorFunction<Long, Order> generatorFunction = new OrderGeneratorFunction();

        return new DataGeneratorSource<>(
                generatorFunction,
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(ordersPerSecond),
                TypeInformation.of(Order.class));
    }

    public static WatermarkStrategy<Order> createWatermarkStrategy(Duration maxOutOfOrderness) {
        return WatermarkStrategy
                .<Order>forBoundedOutOfOrderness(maxOutOfOrderness)
                .withTimestampAssigner((order, timestamp) -> order.getOrderTimeMillis());
    }

    private static class OrderGeneratorFunction implements GeneratorFunction<Long, Order> {
        private static final long serialVersionUID = 1L;

        private transient Random random;
        private transient long baseTime;

        @Override
        public Order map(Long sequence) throws Exception {
            if (random == null) {
                random = new Random(sequence);
                baseTime = System.currentTimeMillis();
            }

            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String customerId = CUSTOMERS[random.nextInt(CUSTOMERS.length)];
            String region = REGIONS[random.nextInt(REGIONS.length)];

            // Select product
            int productIndex = random.nextInt(PRODUCTS.length);
            String productId = PRODUCTS[productIndex][0];
            String productName = PRODUCTS[productIndex][1];
            double unitPrice = Double.parseDouble(PRODUCTS[productIndex][2]);

            // Determine category from product ID prefix
            String category;
            if (productId.startsWith("ELEC")) category = "Electronics";
            else if (productId.startsWith("CLTH")) category = "Clothing";
            else if (productId.startsWith("HOME")) category = "Home & Garden";
            else if (productId.startsWith("SPRT")) category = "Sports";
            else category = "Books";

            int quantity = 1 + random.nextInt(5);
            String status = STATUSES[random.nextInt(STATUSES.length)];

            // Event time with some variance
            long timeOffset = sequence * 100 + random.nextInt(500);
            Timestamp orderTime = Timestamp.from(Instant.ofEpochMilli(baseTime + timeOffset));

            return new Order(
                    orderId,
                    customerId,
                    productId,
                    productName,
                    category,
                    region,
                    quantity,
                    unitPrice,
                    orderTime,
                    status);
        }
    }
}
