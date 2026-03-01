package org.apache.flink.training.exercises.longridealerts;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.training.datatypes.TaxiRide;
import org.apache.flink.training.sources.TaxiRideGenerator;
import org.apache.flink.util.Collector;

/**
 * Exercise 4: Long Ride Alerts (ProcessFunction and Timers)
 *
 * <h2>Learning Objectives</h2>
 * <ul>
 *   <li>Understand ProcessFunction and timers</li>
 *   <li>Implement stateful event-time processing</li>
 *   <li>Handle out-of-order events with state management</li>
 * </ul>
 *
 * <h2>Exercise Description</h2>
 * Detect taxi rides that take longer than 2 hours. For each ride that
 * exceeds this threshold, output an alert.
 *
 * <h2>Instructions</h2>
 * <ol>
 *   <li>Key the ride stream by rideId</li>
 *   <li>When a START event arrives:
 *     <ul>
 *       <li>Store it in state</li>
 *       <li>Register a timer for 2 hours later</li>
 *     </ul>
 *   </li>
 *   <li>When an END event arrives:
 *     <ul>
 *       <li>If START is in state, clear state and delete timer</li>
 *       <li>If timer already fired, ride was already too long</li>
 *     </ul>
 *   </li>
 *   <li>When timer fires:
 *     <ul>
 *       <li>If ride hasn't ended, output an alert</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>KeyedProcessFunction</b>: Low-level API for stateful processing</li>
 *   <li><b>Timers</b>: Schedule callbacks at specific event or processing times</li>
 *   <li><b>onTimer()</b>: Callback when timer fires</li>
 *   <li><b>State management</b>: Track ongoing rides</li>
 * </ul>
 */
public class LongRideAlertsExercise {

    // Alert threshold: 2 hours in milliseconds
    private static final long TWO_HOURS = 2 * 60 * 60 * 1000;

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Generate ride events
        TaxiRideGenerator generator = new TaxiRideGenerator(10000, 1000);
        DataStream<TaxiRide> rides = generator.getSourceStream(env);

        // Detect long rides
        DataStream<Alert> alerts = rides
                .keyBy(ride -> ride.rideId)
                .process(new AlertFunction());

        alerts.print();

        env.execute("Long Ride Alerts Exercise");
    }

    /**
     * Alert record for long rides.
     */
    public static class Alert {
        public long rideId;
        public String message;

        public Alert() {}

        public Alert(long rideId, String message) {
            this.rideId = rideId;
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("ALERT: Ride %d - %s", rideId, message);
        }
    }

    /**
     * ProcessFunction to detect long rides.
     *
     * TODO: Implement this function:
     * - Store START events in state
     * - Register timer 2 hours after START
     * - Clear state and cancel timer when END arrives
     * - Output alert when timer fires (ride too long)
     */
    public static class AlertFunction
            extends KeyedProcessFunction<Long, TaxiRide, Alert> {

        // TODO: Declare state for storing the ride start event
        // private ValueState<TaxiRide> rideState;

        @Override
        public void open(OpenContext openContext) throws Exception {
            // TODO: Initialize state descriptor
            // rideState = getRuntimeContext().getState(
            //     new ValueStateDescriptor<>("ride", TaxiRide.class));
        }

        @Override
        public void processElement(
                TaxiRide ride,
                Context ctx,
                Collector<Alert> out) throws Exception {

            // TODO: Implement the logic
            // 1. If START event:
            //    - Store in state
            //    - Register timer for event time + 2 hours
            // 2. If END event:
            //    - Check if START is in state
            //    - If yes, clear state and delete timer
            throw new RuntimeException("Exercise not implemented");
        }

        @Override
        public void onTimer(
                long timestamp,
                OnTimerContext ctx,
                Collector<Alert> out) throws Exception {

            // TODO: Timer fired - ride has been going for 2+ hours
            // 1. Check if ride is still in state (hasn't ended)
            // 2. If yes, output an alert
            // 3. Clear state
            throw new RuntimeException("Exercise not implemented");
        }
    }
}
