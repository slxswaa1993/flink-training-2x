package org.apache.flink.training.sources;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.training.datatypes.TaxiFare;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * Generates a stream of TaxiFare events using Flink's DataGen connector.
 *
 * <p>This generates fare events that can be joined with TaxiRide events
 * using the rideId field.
 */
public class TaxiFareGenerator {

    private static final String[] PAYMENT_TYPES = {"CASH", "CARD"};

    private final long maxRecords;
    private final int recordsPerSecond;

    public TaxiFareGenerator() {
        this(Long.MAX_VALUE, 10000);
    }

    public TaxiFareGenerator(long maxRecords) {
        this(maxRecords, 10000);
    }

    public TaxiFareGenerator(long maxRecords, int recordsPerSecond) {
        this.maxRecords = maxRecords;
        this.recordsPerSecond = recordsPerSecond;
    }

    /**
     * Creates a DataStream of TaxiFare events.
     *
     * @param env The StreamExecutionEnvironment
     * @return DataStream of TaxiFare events with watermarks
     */
    public DataStream<TaxiFare> getSourceStream(StreamExecutionEnvironment env) {
        DataGeneratorSource<TaxiFare> source = new DataGeneratorSource<>(
                new TaxiFareGeneratorFunction(),
                maxRecords,
                RateLimiterStrategy.perSecond(recordsPerSecond),
                TypeInformation.of(TaxiFare.class)
        );

        return env.fromSource(
                source,
                WatermarkStrategy.<TaxiFare>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((fare, timestamp) -> fare.getStartTimeMillis()),
                "TaxiFare-Source"
        );
    }

    /**
     * Generator function that creates TaxiFare events.
     */
    public static class TaxiFareGeneratorFunction implements GeneratorFunction<Long, TaxiFare> {
        private static final long serialVersionUID = 1L;
        private transient Random random;

        @Override
        public TaxiFare map(Long index) throws Exception {
            if (random == null) {
                random = new Random(index);
            }

            long rideId = index;

            // Generate consistent taxi and driver IDs for same ride
            Random rideRandom = new Random(rideId);
            long taxiId = rideRandom.nextInt(10000) + 1;
            long driverId = rideRandom.nextInt(5000) + 1;

            // Generate fare details
            Instant startTime = Instant.now().minusSeconds(random.nextInt(300));
            String paymentType = PAYMENT_TYPES[random.nextInt(PAYMENT_TYPES.length)];

            // Generate realistic fare amounts
            float baseFare = 2.50f + random.nextFloat() * 50; // Base + distance-based
            float tip = paymentType.equals("CARD") ? baseFare * (0.10f + random.nextFloat() * 0.15f) : 0;
            float tolls = random.nextFloat() < 0.2 ? 5.00f + random.nextFloat() * 10 : 0;
            float totalFare = baseFare + tip + tolls;

            return new TaxiFare(
                    rideId,
                    taxiId,
                    driverId,
                    startTime,
                    paymentType,
                    tip,
                    tolls,
                    totalFare
            );
        }
    }
}
