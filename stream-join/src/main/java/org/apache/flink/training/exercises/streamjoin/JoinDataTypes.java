package org.apache.flink.training.exercises.streamjoin;

import java.io.Serializable;
import java.time.Instant;

/**
 * Data types for the multi-stream join exercise.
 */
public class JoinDataTypes {

    /**
     * Represents an order event (high volume stream).
     */
    public static class OrderEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        public String orderId;
        public String customerId;
        public String productId;
        public int quantity;
        public double amount;
        public Instant orderTime;

        public OrderEvent() {}

        public OrderEvent(String orderId, String customerId, String productId,
                          int quantity, double amount, Instant orderTime) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.productId = productId;
            this.quantity = quantity;
            this.amount = amount;
            this.orderTime = orderTime;
        }

        public long getOrderTimeMillis() {
            return orderTime.toEpochMilli();
        }

        @Override
        public String toString() {
            return String.format("Order{id='%s', customer='%s', product='%s', qty=%d, amount=%.2f}",
                    orderId, customerId, productId, quantity, amount);
        }
    }

    /**
     * Represents a customer update event (moderate volume stream).
     */
    public static class CustomerEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        public String customerId;
        public String name;
        public String email;
        public String tier;  // BRONZE, SILVER, GOLD, PLATINUM
        public String region;
        public Instant updateTime;

        public CustomerEvent() {}

        public CustomerEvent(String customerId, String name, String email,
                             String tier, String region, Instant updateTime) {
            this.customerId = customerId;
            this.name = name;
            this.email = email;
            this.tier = tier;
            this.region = region;
            this.updateTime = updateTime;
        }

        public long getUpdateTimeMillis() {
            return updateTime.toEpochMilli();
        }

        @Override
        public String toString() {
            return String.format("Customer{id='%s', name='%s', tier='%s', region='%s'}",
                    customerId, name, tier, region);
        }
    }

    /**
     * Represents a product catalog entry (low volume, slowly changing).
     */
    public static class ProductEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        public String productId;
        public String name;
        public String category;
        public double price;
        public int stockLevel;
        public Instant updateTime;

        public ProductEvent() {}

        public ProductEvent(String productId, String name, String category,
                            double price, int stockLevel, Instant updateTime) {
            this.productId = productId;
            this.name = name;
            this.category = category;
            this.price = price;
            this.stockLevel = stockLevel;
            this.updateTime = updateTime;
        }

        public long getUpdateTimeMillis() {
            return updateTime.toEpochMilli();
        }

        @Override
        public String toString() {
            return String.format("Product{id='%s', name='%s', category='%s', price=%.2f, stock=%d}",
                    productId, name, category, price, stockLevel);
        }
    }

    /**
     * Enriched order with customer and product information.
     */
    public static class EnrichedOrder implements Serializable {
        private static final long serialVersionUID = 1L;

        public String orderId;
        public Instant orderTime;

        // Order details
        public int quantity;
        public double amount;

        // Customer details
        public String customerId;
        public String customerName;
        public String customerTier;
        public String customerRegion;

        // Product details
        public String productId;
        public String productName;
        public String productCategory;
        public double productPrice;

        public EnrichedOrder() {}

        public EnrichedOrder(OrderEvent order, CustomerEvent customer, ProductEvent product) {
            this.orderId = order.orderId;
            this.orderTime = order.orderTime;
            this.quantity = order.quantity;
            this.amount = order.amount;

            if (customer != null) {
                this.customerId = customer.customerId;
                this.customerName = customer.name;
                this.customerTier = customer.tier;
                this.customerRegion = customer.region;
            } else {
                this.customerId = order.customerId;
                this.customerName = "Unknown";
                this.customerTier = "Unknown";
                this.customerRegion = "Unknown";
            }

            if (product != null) {
                this.productId = product.productId;
                this.productName = product.name;
                this.productCategory = product.category;
                this.productPrice = product.price;
            } else {
                this.productId = order.productId;
                this.productName = "Unknown";
                this.productCategory = "Unknown";
                this.productPrice = 0.0;
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "EnrichedOrder{id='%s', customer='%s' (%s/%s), product='%s' (%s), qty=%d, amount=%.2f}",
                    orderId, customerName, customerTier, customerRegion,
                    productName, productCategory, quantity, amount);
        }
    }
}
