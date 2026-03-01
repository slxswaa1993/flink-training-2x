package org.apache.flink.training.sources;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.training.datatypes.TaxiRide;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * Generates a stream of TaxiRide events using Flink's DataGen connector.
 *
 * <p>This replaces the deprecated SourceFunction API with the new Source API.
 *
 * <p>Key Concepts:
 * <ul>
 *   <li>DataGeneratorSource: New unified source for generating data</li>
 *   <li>GeneratorFunction: Defines how to generate each record</li>
 *   <li>WatermarkStrategy: Assigns timestamps and watermarks</li>
 *   <li>RateLimiter: Controls event generation rate</li>
 * </ul>
 */
public class TaxiRideGenerator {

    // NYC bounding box (approximate)
    private static final float NYC_LON_MIN = -74.05f;
    private static final float NYC_LON_MAX = -73.75f;
    private static final float NYC_LAT_MIN = 40.63f;
    private static final float NYC_LAT_MAX = 40.85f;

    private final long maxRecords;
    private final int recordsPerSecond;

    public TaxiRideGenerator() {
        this(Long.MAX_VALUE, 10000); // Default: unlimited records, 10k/sec
    }

    public TaxiRideGenerator(long maxRecords) {
        this(maxRecords, 10000);
    }

    public TaxiRideGenerator(long maxRecords, int recordsPerSecond) {
        this.maxRecords = maxRecords;
        this.recordsPerSecond = recordsPerSecond;
    }

    /**
     * Creates a DataStream of TaxiRide events.
     *
     * @param env The StreamExecutionEnvironment
     * @return DataStream of TaxiRide events with watermarks
     */
    public DataStream<TaxiRide> getSourceStream(StreamExecutionEnvironment env) {
        DataGeneratorSource<TaxiRide> source = new DataGeneratorSource<>(
                new TaxiRideGeneratorFunction(),
                maxRecords,
                RateLimiterStrategy.perSecond(recordsPerSecond),
                TypeInformation.of(TaxiRide.class)
        );

        return env.fromSource(
                source,
                WatermarkStrategy.<TaxiRide>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner((ride, timestamp) -> ride.getEventTimeMillis()),
                "TaxiRide-Source"
        );
    }

    /**
     * Generator function that creates TaxiRide events.
     */
    public static class TaxiRideGeneratorFunction implements GeneratorFunction<Long, TaxiRide> {
        private static final long serialVersionUID = 1L;
        private transient Random random;

        @Override
        public TaxiRide map(Long index) throws Exception {
            if (random == null) {
                random = new Random(index);
            }

            long rideId = index / 2; // Two events per ride (start and end)
            boolean isStart = (index % 2) == 0;

            // Generate taxi and driver IDs (consistent for same ride)
            Random rideRandom = new Random(rideId);
            long taxiId = rideRandom.nextInt(10000) + 1;
            long driverId = rideRandom.nextInt(5000) + 1;

            // Generate locations
            float startLon = randomLon(rideRandom);
            float startLat = randomLat(rideRandom);
            float endLon = randomLon(rideRandom);
            float endLat = randomLat(rideRandom);

            // Generate event time
            Instant baseTime = Instant.now();
            Instant eventTime = isStart
                    ? baseTime.minusSeconds(random.nextInt(60))
                    : baseTime;

            short passengerCnt = (short) (random.nextInt(4) + 1);

            return new TaxiRide(
                    rideId,
                    taxiId,
                    driverId,
                    isStart,
                    eventTime,
                    startLon,
                    startLat,
                    endLon,
                    endLat,
                    passengerCnt
            );
        }

        private float randomLon(Random rand) {
            return NYC_LON_MIN + rand.nextFloat() * (NYC_LON_MAX - NYC_LON_MIN);
        }

        private float randomLat(Random rand) {
            return NYC_LAT_MIN + rand.nextFloat() * (NYC_LAT_MAX - NYC_LAT_MIN);
        }
    }
}
