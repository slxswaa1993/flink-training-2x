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
 * Solution for Exercise 4: Long Ride Alerts
 *
 * <p>This solution demonstrates:
 * <ul>
 *   <li>Using KeyedProcessFunction for low-level control</li>
 *   <li>Registering and managing event-time timers</li>
 *   <li>Coordinating state and timers for complex patterns</li>
 * </ul>
 */
public class LongRideAlertsSolution {

    private static final long TWO_HOURS = 2 * 60 * 60 * 1000;

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        TaxiRideGenerator generator = new TaxiRideGenerator(10000, 1000);
        DataStream<TaxiRide> rides = generator.getSourceStream(env);

        DataStream<LongRideAlertsExercise.Alert> alerts = rides
                .keyBy(ride -> ride.rideId)
                .process(new AlertFunction());

        alerts.print();

        env.execute("Long Ride Alerts Solution");
    }

    /**
     * Solution: ProcessFunction to detect long rides.
     */
    public static class AlertFunction
            extends KeyedProcessFunction<Long, TaxiRide, LongRideAlertsExercise.Alert> {

        private ValueState<TaxiRide> rideState;

        @Override
        public void open(OpenContext openContext) throws Exception {
            rideState = getRuntimeContext().getState(
                new ValueStateDescriptor<>("ride", TaxiRide.class));
        }

        @Override
        public void processElement(
                TaxiRide ride,
                Context ctx,
                Collector<LongRideAlertsExercise.Alert> out) throws Exception {

            if (ride.isStart) {
                // START event: store in state and register timer
                rideState.update(ride);

                // Register timer for 2 hours after the ride start
                long timerTime = ride.getEventTimeMillis() + TWO_HOURS;
                ctx.timerService().registerEventTimeTimer(timerTime);
            } else {
                // END event: check if we have a matching START
                TaxiRide startRide = rideState.value();
                if (startRide != null) {
                    // Ride ended normally - clear state and delete timer
                    rideState.clear();

                    // Delete the timer
                    long timerTime = startRide.getEventTimeMillis() + TWO_HOURS;
                    ctx.timerService().deleteEventTimeTimer(timerTime);
                }
                // If startRide is null, either:
                // 1. END arrived before START (out of order)
                // 2. Timer already fired and cleared the state
                // In both cases, we don't need to do anything
            }
        }

        @Override
        public void onTimer(
                long timestamp,
                OnTimerContext ctx,
                Collector<LongRideAlertsExercise.Alert> out) throws Exception {

            // Timer fired - check if ride is still ongoing
            TaxiRide ride = rideState.value();
            if (ride != null) {
                // Ride hasn't ended - output alert
                out.collect(new LongRideAlertsExercise.Alert(
                        ride.rideId,
                        "Ride has been ongoing for more than 2 hours!"
                ));

                // Clear state
                rideState.clear();
            }
            // If ride is null, END arrived before timer fired - no alert needed
        }
    }
}
