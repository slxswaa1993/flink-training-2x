package org.apache.flink.training.exercises.ridesandfares;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction;
import org.apache.flink.training.datatypes.TaxiFare;
import org.apache.flink.training.datatypes.TaxiRide;
import org.apache.flink.training.sources.TaxiFareGenerator;
import org.apache.flink.training.sources.TaxiRideGenerator;
import org.apache.flink.util.Collector;

/**
 * Solution for Exercise 2: Rides and Fares
 *
 * <p>This solution demonstrates:
 * <ul>
 *   <li>Connecting two streams with connect()</li>
 *   <li>Using ValueState for stateful processing</li>
 *   <li>Implementing a join pattern with state buffering</li>
 * </ul>
 */
public class RidesAndFaresSolution {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        TaxiRideGenerator rideGenerator = new TaxiRideGenerator(1000, 100);
        TaxiFareGenerator fareGenerator = new TaxiFareGenerator(500, 100);

        DataStream<TaxiRide> rides = rideGenerator.getSourceStream(env)
                .filter(ride -> ride.isStart);

        DataStream<TaxiFare> fares = fareGenerator.getSourceStream(env);

        // Connect streams, key by rideId, and join
        DataStream<RideAndFare> enrichedRides = rides
                .connect(fares)
                .keyBy(ride -> ride.rideId, fare -> fare.rideId)
                .flatMap(new EnrichmentFunction());

        enrichedRides.print();

        env.execute("Rides and Fares Solution");
    }

    /**
     * Solution: Stateful function to join rides with their fares.
     */
    public static class EnrichmentFunction
            extends RichCoFlatMapFunction<TaxiRide, TaxiFare, RideAndFare> {

        private ValueState<TaxiRide> rideState;
        private ValueState<TaxiFare> fareState;

        @Override
        public void open(OpenContext openContext) throws Exception {
            // Initialize state descriptors
            rideState = getRuntimeContext().getState(
                new ValueStateDescriptor<>("ride", TaxiRide.class));
            fareState = getRuntimeContext().getState(
                new ValueStateDescriptor<>("fare", TaxiFare.class));
        }

        @Override
        public void flatMap1(TaxiRide ride, Collector<RideAndFare> out) throws Exception {
            // Process ride events
            TaxiFare fare = fareState.value();
            if (fare != null) {
                // Matching fare already exists - emit and clear state
                out.collect(new RideAndFare(ride, fare));
                fareState.clear();
            } else {
                // No matching fare yet - buffer the ride
                rideState.update(ride);
            }
        }

        @Override
        public void flatMap2(TaxiFare fare, Collector<RideAndFare> out) throws Exception {
            // Process fare events
            TaxiRide ride = rideState.value();
            if (ride != null) {
                // Matching ride already exists - emit and clear state
                out.collect(new RideAndFare(ride, fare));
                rideState.clear();
            } else {
                // No matching ride yet - buffer the fare
                fareState.update(fare);
            }
        }
    }
}
