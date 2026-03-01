package org.apache.flink.training.exercises.sessionanalytics;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents aggregated session statistics.
 *
 * <p>This class holds the computed metrics for a user session after
 * the session window closes.
 */
public class SessionStatistics implements Serializable {
    private static final long serialVersionUID = 1L;

    public String userId;
    public String sessionId;
    public int pageViews;
    public int addToCartCount;
    public int purchaseCount;
    public double totalCartValue;
    public long sessionDurationMs;
    public Instant sessionStart;
    public Instant sessionEnd;

    public SessionStatistics() {}

    public SessionStatistics(
            String userId,
            String sessionId,
            int pageViews,
            int addToCartCount,
            int purchaseCount,
            double totalCartValue,
            long sessionDurationMs,
            Instant sessionStart,
            Instant sessionEnd) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.pageViews = pageViews;
        this.addToCartCount = addToCartCount;
        this.purchaseCount = purchaseCount;
        this.totalCartValue = totalCartValue;
        this.sessionDurationMs = sessionDurationMs;
        this.sessionStart = sessionStart;
        this.sessionEnd = sessionEnd;
    }

    public double getConversionRate() {
        if (pageViews == 0) return 0.0;
        return (double) purchaseCount / pageViews;
    }

    public long getSessionDurationMinutes() {
        return sessionDurationMs / 60000;
    }

    @Override
    public String toString() {
        return String.format(
                "Session{user='%s', session='%s', views=%d, cart=%d, purchases=%d, " +
                        "value=%.2f, duration=%d min, start=%s, end=%s}",
                userId, sessionId, pageViews, addToCartCount, purchaseCount,
                totalCartValue, getSessionDurationMinutes(), sessionStart, sessionEnd);
    }
}
