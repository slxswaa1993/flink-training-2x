package org.apache.flink.training.exercises.hourlytips;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.training.datatypes.TaxiFare;
import org.apache.flink.training.sources.TaxiFareGenerator;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * Exercise 3: Hourly Tips (Windowed Analytics)
 *
 * <h2>Learning Objectives</h2>
 * <ul>
 *   <li>Understand event time and watermarks</li>
 *   <li>Use tumbling windows for time-based aggregation</li>
 *   <li>Find the maximum value across all windows</li>
 * </ul>
 *
 * <h2>Exercise Description</h2>
 * Find the driver earning the most tips per hour. The output should be:
 * (windowEndTime, driverId, maxTips)
 *
 * <h2>Instructions</h2>
 * <ol>
 *   <li>Key the fare stream by driverId</li>
 *   <li>Apply a 1-hour tumbling window</li>
 *   <li>Sum tips per driver per window</li>
 *   <li>Find the maximum across all drivers in each window</li>
 * </ol>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Event Time</b>: Processing based on event timestamps</li>
 *   <li><b>Watermarks</b>: Signal progress of event time</li>
 *   <li><b>TumblingEventTimeWindows</b>: Non-overlapping fixed-size windows</li>
 *   <li><b>AggregateFunction</b>: Incremental aggregation for efficiency</li>
 *   <li><b>ProcessWindowFunction</b>: Access window metadata</li>
 * </ul>
 */
public class HourlyTipsExercise {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Generate fare stream with event time
        TaxiFareGenerator fareGenerator = new TaxiFareGenerator(10000, 1000);
        DataStream<TaxiFare> fares = fareGenerator.getSourceStream(env);

        // TODO: Implement the hourly tips calculation
        // Step 1: Calculate tips per driver per hour
        DataStream<Tuple3<Long, Long, Float>> hourlyTips = fares
                .keyBy(fare -> fare.driverId)
                .window(TumblingEventTimeWindows.of(Duration.ofHours(1)))
                // TODO: Apply aggregation to sum tips
                // Hint: Use .aggregate(aggregateFunction, processWindowFunction)
                .aggregate(new TipSumAggregate(), new TipWindowFunction());

        // Step 2: Find the driver with the maximum tips per window
        // TODO: Key by window end time and find max
        DataStream<Tuple3<Long, Long, Float>> hourlyMax = hourlyTips
                .keyBy(t -> t.f0) // Key by window end time
                .maxBy(2); // Max by tip amount

        hourlyMax.print();

        env.execute("Hourly Tips Exercise");
    }

    /**
     * Aggregates tips for a single driver in a window.
     *
     * TODO: Implement this AggregateFunction to:
     * - Accumulate tip amounts
     * - Return total tips for the window
     */
    public static class TipSumAggregate implements AggregateFunction<TaxiFare, Float, Float> {

        @Override
        public Float createAccumulator() {
            // TODO: Initialize accumulator
            return 0f;
        }

        @Override
        public Float add(TaxiFare fare, Float accumulator) {
            // TODO: Add fare tip to accumulator
            return accumulator + fare.tip;
        }

        @Override
        public Float getResult(Float accumulator) {
            // TODO: Return final result
            return accumulator;
        }

        @Override
        public Float merge(Float a, Float b) {
            // TODO: Merge accumulators (for session windows)
            return a + b;
        }
    }

    /**
     * Wraps the aggregation result with window metadata.
     *
     * Outputs: (windowEndTime, driverId, totalTips)
     */
    public static class TipWindowFunction
            extends ProcessWindowFunction<Float, Tuple3<Long, Long, Float>, Long, TimeWindow> {

        @Override
        public void process(
                Long driverId,
                Context context,
                Iterable<Float> elements,
                Collector<Tuple3<Long, Long, Float>> out) {

            Float totalTips = elements.iterator().next();
            long windowEnd = context.window().getEnd();
            out.collect(Tuple3.of(windowEnd, driverId, totalTips));
        }
    }
}
