package org.apache.flink.training.exercises.streamjoin;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.training.exercises.streamjoin.JoinDataTypes.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * Generators for multi-stream join exercise data.
 */
public class JoinDataGenerators {

    private static final String[] CUSTOMER_IDS = {
            "C001", "C002", "C003", "C004", "C005", "C006", "C007", "C008", "C009", "C010"
    };

    private static final String[] PRODUCT_IDS = {
            "P001", "P002", "P003", "P004", "P005"
    };

    private static final String[] TIERS = {"BRONZE", "SILVER", "GOLD", "PLATINUM"};
    private static final String[] REGIONS = {"US-EAST", "US-WEST", "EU", "ASIA"};
    private static final String[] CATEGORIES = {"Electronics", "Clothing", "Home", "Sports", "Books"};

    // ========== Order Generator (High Volume) ==========

    public static DataGeneratorSource<OrderEvent> createOrderSource(double eventsPerSecond) {
        return new DataGeneratorSource<>(
                new OrderGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(eventsPerSecond),
                TypeInformation.of(OrderEvent.class));
    }

    public static WatermarkStrategy<OrderEvent> createOrderWatermarkStrategy(Duration maxOutOfOrderness) {
        return WatermarkStrategy
                .<OrderEvent>forBoundedOutOfOrderness(maxOutOfOrderness)
                .withTimestampAssigner((event, timestamp) -> event.getOrderTimeMillis());
    }

    private static class OrderGeneratorFunction implements GeneratorFunction<Long, OrderEvent> {
        private static final long serialVersionUID = 1L;
        private transient Random random;
        private transient long baseTime;

        @Override
        public OrderEvent map(Long sequence) throws Exception {
            if (random == null) {
                random = new Random(sequence);
                baseTime = System.currentTimeMillis();
            }

            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String customerId = CUSTOMER_IDS[random.nextInt(CUSTOMER_IDS.length)];
            String productId = PRODUCT_IDS[random.nextInt(PRODUCT_IDS.length)];
            int quantity = 1 + random.nextInt(5);
            double amount = quantity * (10 + random.nextDouble() * 490);

            long timeOffset = sequence * 100 + random.nextInt(200);
            Instant orderTime = Instant.ofEpochMilli(baseTime + timeOffset);

            return new OrderEvent(orderId, customerId, productId, quantity, amount, orderTime);
        }
    }

    // ========== Customer Generator (Moderate Volume) ==========

    public static DataGeneratorSource<CustomerEvent> createCustomerSource(double eventsPerSecond) {
        return new DataGeneratorSource<>(
                new CustomerGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(eventsPerSecond),
                TypeInformation.of(CustomerEvent.class));
    }

    public static WatermarkStrategy<CustomerEvent> createCustomerWatermarkStrategy(Duration maxOutOfOrderness) {
        return WatermarkStrategy
                .<CustomerEvent>forBoundedOutOfOrderness(maxOutOfOrderness)
                .withTimestampAssigner((event, timestamp) -> event.getUpdateTimeMillis());
    }

    private static class CustomerGeneratorFunction implements GeneratorFunction<Long, CustomerEvent> {
        private static final long serialVersionUID = 1L;
        private transient Random random;
        private transient long baseTime;

        @Override
        public CustomerEvent map(Long sequence) throws Exception {
            if (random == null) {
                random = new Random(sequence);
                baseTime = System.currentTimeMillis();
            }

            String customerId = CUSTOMER_IDS[(int) (sequence % CUSTOMER_IDS.length)];
            String name = "Customer " + customerId.substring(1);
            String email = customerId.toLowerCase() + "@example.com";
            String tier = TIERS[random.nextInt(TIERS.length)];
            String region = REGIONS[random.nextInt(REGIONS.length)];

            long timeOffset = sequence * 500 + random.nextInt(1000);
            Instant updateTime = Instant.ofEpochMilli(baseTime + timeOffset);

            return new CustomerEvent(customerId, name, email, tier, region, updateTime);
        }
    }

    // ========== Product Generator (Low Volume) ==========

    public static DataGeneratorSource<ProductEvent> createProductSource(double eventsPerSecond) {
        return new DataGeneratorSource<>(
                new ProductGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(eventsPerSecond),
                TypeInformation.of(ProductEvent.class));
    }

    public static WatermarkStrategy<ProductEvent> createProductWatermarkStrategy(Duration maxOutOfOrderness) {
        return WatermarkStrategy
                .<ProductEvent>forBoundedOutOfOrderness(maxOutOfOrderness)
                .withTimestampAssigner((event, timestamp) -> event.getUpdateTimeMillis());
    }

    private static class ProductGeneratorFunction implements GeneratorFunction<Long, ProductEvent> {
        private static final long serialVersionUID = 1L;
        private transient Random random;
        private transient long baseTime;

        @Override
        public ProductEvent map(Long sequence) throws Exception {
            if (random == null) {
                random = new Random(sequence);
                baseTime = System.currentTimeMillis();
            }

            String productId = PRODUCT_IDS[(int) (sequence % PRODUCT_IDS.length)];
            String name = "Product " + productId.substring(1);
            String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
            double price = 10 + random.nextDouble() * 490;
            int stockLevel = random.nextInt(1000);

            long timeOffset = sequence * 2000 + random.nextInt(5000);
            Instant updateTime = Instant.ofEpochMilli(baseTime + timeOffset);

            return new ProductEvent(productId, name, category, price, stockLevel, updateTime);
        }
    }
}
