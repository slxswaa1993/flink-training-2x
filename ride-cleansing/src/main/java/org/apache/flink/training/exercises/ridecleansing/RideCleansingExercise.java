package org.apache.flink.training.exercises.ridecleansing;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.training.datatypes.TaxiRide;
import org.apache.flink.training.sources.TaxiRideGenerator;
import org.apache.flink.training.utils.GeoUtils;

/**
 * Exercise 1: Ride Cleansing
 *
 * <h2>Learning Objectives</h2>
 * <ul>
 *   <li>Understand DataStream filtering with the filter() operator</li>
 *   <li>Learn to use predicate functions for data cleansing</li>
 *   <li>Practice with geographic data validation</li>
 * </ul>
 *
 * <h2>Exercise Description</h2>
 * The task is to filter the taxi ride stream to only keep rides that
 * both start AND end within NYC boundaries.
 *
 * <h2>Instructions</h2>
 * <ol>
 *   <li>Use the GeoUtils.isInNYC() method to check if coordinates are in NYC</li>
 *   <li>Apply a filter() transformation to keep only valid rides</li>
 *   <li>A ride is valid if BOTH start and end locations are in NYC</li>
 * </ol>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>filter()</b>: Keeps elements that satisfy a predicate</li>
 *   <li><b>Lambda expressions</b>: Concise way to define filter logic</li>
 *   <li><b>Data cleansing</b>: Common first step in data pipelines</li>
 * </ul>
 */
public class RideCleansingExercise {

    public static void main(String[] args) throws Exception {
        // Set up the execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Create the source stream
        TaxiRideGenerator generator = new TaxiRideGenerator(1000, 100);
        DataStream<TaxiRide> rides = generator.getSourceStream(env);

        // TODO: Implement the cleansing logic
        // Filter to keep only rides that start AND end within NYC
        DataStream<TaxiRide> cleanedRides = rides
                // EXERCISE: Replace this with proper filtering logic
                // Hint: Use GeoUtils.isInNYC(lon, lat) to check each location
                .filter(ride -> {
                    return GeoUtils.isInNYC(ride.startLon, ride.startLat) && GeoUtils.isInNYC(ride.endLon, ride.endLat);
                });

        // Print the results
        cleanedRides.print();

        // Execute the job
        env.execute("Ride Cleansing Exercise");
    }

    /**
     * Exception thrown when the exercise solution is not yet implemented.
     */
    public static class MissingSolutionException extends RuntimeException {
        public MissingSolutionException() {
            super("This exercise has not been implemented yet. " +
                  "Replace the placeholder code with your solution.");
        }
    }
}
