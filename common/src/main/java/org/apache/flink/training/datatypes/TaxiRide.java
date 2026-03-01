package org.apache.flink.training.datatypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A TaxiRide represents a taxi ride event.
 *
 * <p>Each ride is identified by a unique rideId and consists of two events:
 * a trip start event and a trip end event. Both events share the same rideId.
 *
 * <p>Schema:
 * <ul>
 *   <li>rideId: unique identifier for the ride</li>
 *   <li>taxiId: unique identifier for the taxi</li>
 *   <li>driverId: unique identifier for the driver</li>
 *   <li>isStart: true for start events, false for end events</li>
 *   <li>eventTime: timestamp when the event occurred</li>
 *   <li>startLon/startLat: longitude/latitude of ride start</li>
 *   <li>endLon/endLat: longitude/latitude of ride end</li>
 *   <li>passengerCnt: number of passengers</li>
 * </ul>
 */
public class TaxiRide implements Serializable {
    private static final long serialVersionUID = 1L;

    public long rideId;
    public long taxiId;
    public long driverId;
    public boolean isStart;
    public Instant eventTime;
    public float startLon;
    public float startLat;
    public float endLon;
    public float endLat;
    public short passengerCnt;

    public TaxiRide() {
        this.eventTime = Instant.now();
    }

    public TaxiRide(
            long rideId,
            long taxiId,
            long driverId,
            boolean isStart,
            Instant eventTime,
            float startLon,
            float startLat,
            float endLon,
            float endLat,
            short passengerCnt) {
        this.rideId = rideId;
        this.taxiId = taxiId;
        this.driverId = driverId;
        this.isStart = isStart;
        this.eventTime = eventTime;
        this.startLon = startLon;
        this.startLat = startLat;
        this.endLon = endLon;
        this.endLat = endLat;
        this.passengerCnt = passengerCnt;
    }

    /**
     * Gets the event timestamp in milliseconds for watermark assignment.
     */
    public long getEventTimeMillis() {
        return eventTime.toEpochMilli();
    }

    /**
     * Returns the start location as a string.
     */
    public String getStartLocation() {
        return String.format("(%.5f, %.5f)", startLon, startLat);
    }

    /**
     * Returns the end location as a string.
     */
    public String getEndLocation() {
        return String.format("(%.5f, %.5f)", endLon, endLat);
    }

    @Override
    public String toString() {
        return String.format(
                "TaxiRide{rideId=%d, taxiId=%d, driverId=%d, isStart=%s, time=%s, start=%s, end=%s, passengers=%d}",
                rideId, taxiId, driverId, isStart, eventTime, getStartLocation(), getEndLocation(), passengerCnt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxiRide taxiRide = (TaxiRide) o;
        return rideId == taxiRide.rideId &&
                taxiId == taxiRide.taxiId &&
                driverId == taxiRide.driverId &&
                isStart == taxiRide.isStart &&
                Float.compare(taxiRide.startLon, startLon) == 0 &&
                Float.compare(taxiRide.startLat, startLat) == 0 &&
                Float.compare(taxiRide.endLon, endLon) == 0 &&
                Float.compare(taxiRide.endLat, endLat) == 0 &&
                passengerCnt == taxiRide.passengerCnt &&
                Objects.equals(eventTime, taxiRide.eventTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rideId, taxiId, driverId, isStart, eventTime,
                startLon, startLat, endLon, endLat, passengerCnt);
    }
}
