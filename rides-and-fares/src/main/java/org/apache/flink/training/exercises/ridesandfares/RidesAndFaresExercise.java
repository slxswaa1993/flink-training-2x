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
 * Exercise 2: Rides and Fares (Stateful Enrichment)
 *
 * <h2>Learning Objectives</h2>
 * <ul>
 *   <li>Connect two streams using connect()</li>
 *   <li>Implement stateful processing with KeyedState</li>
 *   <li>Handle out-of-order events with state buffering</li>
 * </ul>
 *
 * <h2>Exercise Description</h2>
 * Join the TaxiRide stream with the TaxiFare stream on rideId.
 * Since events may arrive out of order, you need to use state to
 * buffer events until their matching partner arrives.
 *
 * <h2>Instructions</h2>
 * <ol>
 *   <li>Connect the ride and fare streams</li>
 *   <li>Key both streams by rideId</li>
 *   <li>Implement a CoFlatMapFunction that:
 *     <ul>
 *       <li>Stores rides/fares in state while waiting for the match</li>
 *       <li>Outputs a RideAndFare when both arrive</li>
 *       <li>Clears state after joining</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>connect()</b>: Creates a ConnectedStream from two DataStreams</li>
 *   <li><b>keyBy()</b>: Partitions stream by key for stateful processing</li>
 *   <li><b>ValueState</b>: Single-value state for storing buffered events</li>
 *   <li><b>RichCoFlatMapFunction</b>: Process function for connected streams</li>
 * </ul>
 */
public class RidesAndFaresExercise {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Generate ride and fare streams
        TaxiRideGenerator rideGenerator = new TaxiRideGenerator(1000, 100);
        TaxiFareGenerator fareGenerator = new TaxiFareGenerator(500, 100);

        DataStream<TaxiRide> rides = rideGenerator.getSourceStream(env)
                .filter(ride -> ride.isStart); // Only use start events

        DataStream<TaxiFare> fares = fareGenerator.getSourceStream(env);

        // TODO: Implement the join logic
        // 1. Connect the two streams
        // 2. Key by rideId
        // 3. Apply the EnrichmentFunction (implement it below)
        DataStream<RideAndFare> enrichedRides = rides
                .connect(fares)
                .keyBy(ride -> ride.rideId, fare -> fare.rideId)
                .flatMap(new EnrichmentFunction());

        enrichedRides.print();

        env.execute("Rides and Fares Exercise");
    }

    /**
     * Stateful function to join rides with their fares.
     *
     * TODO: Implement this function:
     * - Use ValueState to store pending rides and fares
     * - When a ride arrives, check if fare is buffered; if so, output and clear
     * - When a fare arrives, check if ride is buffered; if so, output and clear
     */
    public static class EnrichmentFunction
            extends RichCoFlatMapFunction<TaxiRide, TaxiFare, RideAndFare> {

        // TODO: Declare ValueState for buffering rides and fares
        // private ValueState<TaxiRide> rideState;
        // private ValueState<TaxiFare> fareState;

        @Override
        public void open(OpenContext openContext) throws Exception {
            // TODO: Initialize state descriptors
            // rideState = getRuntimeContext().getState(
            //     new ValueStateDescriptor<>("ride", TaxiRide.class));
            // fareState = getRuntimeContext().getState(
            //     new ValueStateDescriptor<>("fare", TaxiFare.class));
        }

        @Override
        public void flatMap1(TaxiRide ride, Collector<RideAndFare> out) throws Exception {
            // TODO: Process ride events
            // 1. Check if matching fare is in state
            // 2. If yes, output RideAndFare and clear state
            // 3. If no, store ride in state
            throw new RuntimeException("Exercise not implemented");
        }

        @Override
        public void flatMap2(TaxiFare fare, Collector<RideAndFare> out) throws Exception {
            // TODO: Process fare events
            // 1. Check if matching ride is in state
            // 2. If yes, output RideAndFare and clear state
            // 3. If no, store fare in state
            throw new RuntimeException("Exercise not implemented");
        }
    }
}
