package org.apache.flink.training.exercises.sessionanalytics;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * A generator for UserEvent data.
 *
 * <p>Generates realistic e-commerce user events including page views,
 * add-to-cart actions, and purchases. Simulates user sessions with
 * realistic timing patterns.
 */
public class UserEventGenerator {

    // Only 3 users to create natural session gaps (events rotate among users)
    private static final String[] USERS = {"user_001", "user_002", "user_003"};

    private static final String[] PAGES = {"/home", "/products", "/product/1", "/product/2", "/product/3",
            "/cart", "/checkout", "/confirmation", "/category/electronics", "/category/clothing"};

    private static final String[] PRODUCTS = {"PROD_001", "PROD_002", "PROD_003", "PROD_004", "PROD_005"};

    private static final double[] PRICES = {19.99, 49.99, 99.99, 149.99, 299.99};

    /**
     * Creates a DataGeneratorSource for UserEvents.
     *
     * @param eventsPerSecond rate of event generation
     * @return configured DataGeneratorSource
     */
    public static DataGeneratorSource<UserEvent> createSource(double eventsPerSecond) {
        GeneratorFunction<Long, UserEvent> generatorFunction = new UserEventGeneratorFunction();

        return new DataGeneratorSource<>(
                generatorFunction,
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(eventsPerSecond),
                TypeInformation.of(UserEvent.class));
    }

    /**
     * Creates a WatermarkStrategy for UserEvents with out-of-orderness tolerance.
     *
     * @param maxOutOfOrderness maximum expected out-of-orderness
     * @return configured WatermarkStrategy
     */
    public static WatermarkStrategy<UserEvent> createWatermarkStrategy(Duration maxOutOfOrderness) {
        return WatermarkStrategy
                .<UserEvent>forBoundedOutOfOrderness(maxOutOfOrderness)
                .withTimestampAssigner((event, timestamp) -> event.getEventTimeMillis());
    }

    /**
     * Generator function that creates realistic UserEvent sequences.
     * Creates bursts of events with gaps to demonstrate session windows.
     */
    private static class UserEventGeneratorFunction implements GeneratorFunction<Long, UserEvent> {
        private static final long serialVersionUID = 1L;

        private transient Random random;
        private transient long baseTime;
        private transient String currentBurstUser;
        private transient int eventsInBurst;

        @Override
        public UserEvent map(Long sequence) throws Exception {
            if (random == null) {
                random = new Random(sequence);
                baseTime = System.currentTimeMillis();
                currentBurstUser = USERS[0];
                eventsInBurst = 0;
            }

            // Create bursts: 5-10 events for one user, then switch
            // This creates natural gaps when a user isn't active
            if (eventsInBurst == 0 || eventsInBurst >= 5 + random.nextInt(6)) {
                currentBurstUser = USERS[random.nextInt(USERS.length)];
                eventsInBurst = 0;
            }
            eventsInBurst++;

            String userId = currentBurstUser;
            String sessionId = userId + "_session_" + (sequence / 100);

            // Determine event type based on weighted probability
            UserEvent.EventType eventType = generateEventType();

            String pageUrl = PAGES[random.nextInt(PAGES.length)];
            String productId = null;
            double cartValue = 0.0;

            if (eventType == UserEvent.EventType.ADD_TO_CART ||
                    eventType == UserEvent.EventType.PURCHASE) {
                int productIndex = random.nextInt(PRODUCTS.length);
                productId = PRODUCTS[productIndex];
                cartValue = PRICES[productIndex] * (1 + random.nextInt(3));
            }

            // Time advances faster: ~20ms per event for quick demo
            // Events for same user cluster together to form sessions
            long timeOffset = sequence * 20 + random.nextInt(100);

            // 5% of events arrive late (up to 3 seconds late for demo)
            if (random.nextDouble() < 0.05) {
                timeOffset -= random.nextInt(3000);
            }

            Instant eventTime = Instant.ofEpochMilli(baseTime + timeOffset);

            return new UserEvent(
                    userId,
                    sessionId,
                    eventType,
                    pageUrl,
                    productId,
                    cartValue,
                    eventTime);
        }

        private UserEvent.EventType generateEventType() {
            double rand = random.nextDouble();
            if (rand < 0.70) {
                return UserEvent.EventType.PAGE_VIEW;
            } else if (rand < 0.85) {
                return UserEvent.EventType.ADD_TO_CART;
            } else if (rand < 0.95) {
                return UserEvent.EventType.REMOVE_FROM_CART;
            } else {
                return UserEvent.EventType.PURCHASE;
            }
        }
    }
}
