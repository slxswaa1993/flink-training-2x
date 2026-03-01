package org.apache.flink.training.datatypes;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A TaxiFare represents fare information for a taxi ride.
 *
 * <p>The fare is associated with a ride through the rideId field.
 *
 * <p>Schema:
 * <ul>
 *   <li>rideId: unique identifier matching the TaxiRide</li>
 *   <li>taxiId: unique identifier for the taxi</li>
 *   <li>driverId: unique identifier for the driver</li>
 *   <li>startTime: when the ride started</li>
 *   <li>paymentType: "CASH" or "CARD"</li>
 *   <li>tip: tip amount</li>
 *   <li>tolls: toll charges</li>
 *   <li>totalFare: total fare amount</li>
 * </ul>
 */
public class TaxiFare implements Serializable {
    private static final long serialVersionUID = 1L;

    public long rideId;
    public long taxiId;
    public long driverId;
    public Instant startTime;
    public String paymentType;
    public float tip;
    public float tolls;
    public float totalFare;

    public TaxiFare() {
        this.startTime = Instant.now();
    }

    public TaxiFare(
            long rideId,
            long taxiId,
            long driverId,
            Instant startTime,
            String paymentType,
            float tip,
            float tolls,
            float totalFare) {
        this.rideId = rideId;
        this.taxiId = taxiId;
        this.driverId = driverId;
        this.startTime = startTime;
        this.paymentType = paymentType;
        this.tip = tip;
        this.tolls = tolls;
        this.totalFare = totalFare;
    }

    /**
     * Gets the start timestamp in milliseconds for watermark assignment.
     */
    public long getStartTimeMillis() {
        return startTime.toEpochMilli();
    }

    @Override
    public String toString() {
        return String.format(
                "TaxiFare{rideId=%d, taxiId=%d, driverId=%d, startTime=%s, paymentType=%s, tip=%.2f, tolls=%.2f, totalFare=%.2f}",
                rideId, taxiId, driverId, startTime, paymentType, tip, tolls, totalFare);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxiFare taxiFare = (TaxiFare) o;
        return rideId == taxiFare.rideId &&
                taxiId == taxiFare.taxiId &&
                driverId == taxiFare.driverId &&
                Float.compare(taxiFare.tip, tip) == 0 &&
                Float.compare(taxiFare.tolls, tolls) == 0 &&
                Float.compare(taxiFare.totalFare, totalFare) == 0 &&
                Objects.equals(startTime, taxiFare.startTime) &&
                Objects.equals(paymentType, taxiFare.paymentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rideId, taxiId, driverId, startTime, paymentType, tip, tolls, totalFare);
    }
}
