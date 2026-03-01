package org.apache.flink.training.exercises.sessionanalytics;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A UserEvent represents a user activity on an e-commerce website.
 *
 * <p>Events include page views, add-to-cart actions, and purchases.
 * Used for session window analytics exercises.
 */
public class UserEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EventType {
        PAGE_VIEW,
        ADD_TO_CART,
        REMOVE_FROM_CART,
        PURCHASE
    }

    public String userId;
    public String sessionId;
    public EventType eventType;
    public String pageUrl;
    public String productId;
    public double cartValue;
    public Instant eventTime;

    public UserEvent() {
        this.eventTime = Instant.now();
    }

    public UserEvent(
            String userId,
            String sessionId,
            EventType eventType,
            String pageUrl,
            String productId,
            double cartValue,
            Instant eventTime) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.pageUrl = pageUrl;
        this.productId = productId;
        this.cartValue = cartValue;
        this.eventTime = eventTime;
    }

    public long getEventTimeMillis() {
        return eventTime.toEpochMilli();
    }

    @Override
    public String toString() {
        return String.format(
                "UserEvent{userId='%s', session='%s', type=%s, page='%s', product='%s', cart=%.2f, time=%s}",
                userId, sessionId, eventType, pageUrl, productId, cartValue, eventTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEvent userEvent = (UserEvent) o;
        return Double.compare(userEvent.cartValue, cartValue) == 0 &&
                Objects.equals(userId, userEvent.userId) &&
                Objects.equals(sessionId, userEvent.sessionId) &&
                eventType == userEvent.eventType &&
                Objects.equals(pageUrl, userEvent.pageUrl) &&
                Objects.equals(productId, userEvent.productId) &&
                Objects.equals(eventTime, userEvent.eventTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, sessionId, eventType, pageUrl, productId, cartValue, eventTime);
    }
}
