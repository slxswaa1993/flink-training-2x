package org.apache.flink.training.exercises.sessionanalytics;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Instant;

/**
 * Process function for aggregating UserEvents within a session window.
 *
 * <p>This function demonstrates how to use ProcessWindowFunction to:
 * <ul>
 *   <li>Access all events in a window</li>
 *   <li>Get window metadata (start/end time)</li>
 *   <li>Compute aggregations across the window</li>
 * </ul>
 */
public class SessionWindowFunction
        extends ProcessWindowFunction<UserEvent, SessionStatistics, String, TimeWindow> {

    @Override
    public void process(
            String userId,
            Context context,
            Iterable<UserEvent> events,
            Collector<SessionStatistics> out) {

        int pageViews = 0;
        int addToCartCount = 0;
        int purchaseCount = 0;
        double totalCartValue = 0.0;
        String sessionId = null;

        // Aggregate all events in the session
        for (UserEvent event : events) {
            if (sessionId == null) {
                sessionId = event.sessionId;
            }

            switch (event.eventType) {
                case PAGE_VIEW:
                    pageViews++;
                    break;
                case ADD_TO_CART:
                    addToCartCount++;
                    totalCartValue += event.cartValue;
                    break;
                case REMOVE_FROM_CART:
                    totalCartValue = Math.max(0, totalCartValue - event.cartValue);
                    break;
                case PURCHASE:
                    purchaseCount++;
                    break;
            }
        }

        // Get window timing information
        TimeWindow window = context.window();
        long sessionDurationMs = window.getEnd() - window.getStart();
        Instant sessionStart = Instant.ofEpochMilli(window.getStart());
        Instant sessionEnd = Instant.ofEpochMilli(window.getEnd());

        SessionStatistics stats = new SessionStatistics(
                userId,
                sessionId != null ? sessionId : "unknown",
                pageViews,
                addToCartCount,
                purchaseCount,
                totalCartValue,
                sessionDurationMs,
                sessionStart,
                sessionEnd);

        out.collect(stats);
    }
}
