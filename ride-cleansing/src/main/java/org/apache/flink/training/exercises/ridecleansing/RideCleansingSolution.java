package org.apache.flink.training.exercises.ridecleansing;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.training.datatypes.TaxiRide;
import org.apache.flink.training.sources.TaxiRideGenerator;
import org.apache.flink.training.utils.GeoUtils;

/**
 * Solution for Exercise 1: Ride Cleansing
 *
 * <p>This solution demonstrates:
 * <ul>
 *   <li>Using filter() to cleanse data streams</li>
 *   <li>Applying geographic validation</li>
 *   <li>Working with the TaxiRide data type</li>
 * </ul>
 */
public class RideCleansingSolution {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        TaxiRideGenerator generator = new TaxiRideGenerator(1000, 100);
        DataStream<TaxiRide> rides = generator.getSourceStream(env);

        // Solution: Filter rides where both start AND end are in NYC
        DataStream<TaxiRide> cleanedRides = rides
                .filter(ride -> isValidRide(ride));

        cleanedRides.print();

        env.execute("Ride Cleansing Solution");
    }

    /**
     * Validates that a ride starts and ends within NYC.
     *
     * @param ride The TaxiRide to validate
     * @return true if the ride is valid (both locations in NYC)
     */
    public static boolean isValidRide(TaxiRide ride) {
        return GeoUtils.isInNYC(ride.startLon, ride.startLat) &&
               GeoUtils.isInNYC(ride.endLon, ride.endLat);
    }
}
