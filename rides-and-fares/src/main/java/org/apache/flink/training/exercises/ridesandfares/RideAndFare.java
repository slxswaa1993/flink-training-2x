package org.apache.flink.training.exercises.ridesandfares;

import org.apache.flink.training.datatypes.TaxiFare;
import org.apache.flink.training.datatypes.TaxiRide;

import java.io.Serializable;
import java.util.Objects;

/**
 * A RideAndFare represents a joined record of a TaxiRide and its corresponding TaxiFare.
 */
public class RideAndFare implements Serializable {
    private static final long serialVersionUID = 1L;

    public TaxiRide ride;
    public TaxiFare fare;

    public RideAndFare() {}

    public RideAndFare(TaxiRide ride, TaxiFare fare) {
        this.ride = ride;
        this.fare = fare;
    }

    @Override
    public String toString() {
        return String.format("RideAndFare{rideId=%d, fare=%.2f, tip=%.2f}",
                ride != null ? ride.rideId : -1,
                fare != null ? fare.totalFare : 0,
                fare != null ? fare.tip : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RideAndFare that = (RideAndFare) o;
        return Objects.equals(ride, that.ride) && Objects.equals(fare, that.fare);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ride, fare);
    }
}
