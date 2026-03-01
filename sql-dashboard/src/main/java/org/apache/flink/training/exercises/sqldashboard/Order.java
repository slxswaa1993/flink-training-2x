package org.apache.flink.training.exercises.sqldashboard;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents an e-commerce order.
 *
 * <p>Used for real-time dashboard analytics with Flink SQL.
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    public String orderId;
    public String customerId;
    public String productId;
    public String productName;
    public String category;
    public String region;
    public int quantity;
    public double unitPrice;
    public double totalAmount;
    public Timestamp orderTime;
    public String status;

    public Order() {
        this.orderTime = Timestamp.from(Instant.now());
    }

    public Order(
            String orderId,
            String customerId,
            String productId,
            String productName,
            String category,
            String region,
            int quantity,
            double unitPrice,
            Timestamp orderTime,
            String status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.region = region;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = quantity * unitPrice;
        this.orderTime = orderTime;
        this.status = status;
    }

    public long getOrderTimeMillis() {
        return orderTime.getTime();
    }

    @Override
    public String toString() {
        return String.format(
                "Order{id='%s', customer='%s', product='%s', region='%s', qty=%d, total=%.2f, time=%s, status='%s'}",
                orderId, customerId, productName, region, quantity, totalAmount, orderTime, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}
