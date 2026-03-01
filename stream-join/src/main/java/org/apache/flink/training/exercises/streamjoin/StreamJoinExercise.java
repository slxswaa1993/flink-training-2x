package org.apache.flink.training.exercises.streamjoin;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.training.exercises.streamjoin.JoinDataTypes.*;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * Multi-Stream Join Exercise (Phase 5.2)
 *
 * <p>This exercise teaches you different join patterns in Flink:
 * <ul>
 *   <li>Stream-stream join with time windows</li>
 *   <li>Temporal table join for slowly changing dimensions</li>
 *   <li>Broadcast state pattern for small lookup tables</li>
 * </ul>
 *
 * <h2>Exercise Goals:</h2>
 * <ol>
 *   <li>Join high-volume orders with customer information</li>
 *   <li>Enrich orders with product catalog data</li>
 *   <li>Handle late-arriving dimension updates</li>
 *   <li>Choose appropriate join strategy for each use case</li>
 * </ol>
 *
 * <h2>Join Strategies:</h2>
 * <ul>
 *   <li><b>Window Join:</b> For streams with similar event rates, join within time window</li>
 *   <li><b>Interval Join:</b> For streams where events are correlated within a time range</li>
 *   <li><b>Temporal Join:</b> Join with the version of a dimension valid at event time</li>
 *   <li><b>Broadcast Join:</b> For small, slowly-changing lookup tables</li>
 * </ul>
 */
public class StreamJoinExercise {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(10000);

        // Create the three streams with different rates
        // Orders: high volume (10/sec)
        DataGeneratorSource<OrderEvent> orderSource =
                JoinDataGenerators.createOrderSource(10.0);
        WatermarkStrategy<OrderEvent> orderWatermark =
                JoinDataGenerators.createOrderWatermarkStrategy(Duration.ofSeconds(5));

        // Customers: moderate volume (2/sec)
        DataGeneratorSource<CustomerEvent> customerSource =
                JoinDataGenerators.createCustomerSource(2.0);
        WatermarkStrategy<CustomerEvent> customerWatermark =
                JoinDataGenerators.createCustomerWatermarkStrategy(Duration.ofSeconds(5));

        // Products: low volume (0.5/sec) - suitable for broadcast
        DataGeneratorSource<ProductEvent> productSource =
                JoinDataGenerators.createProductSource(0.5);
        WatermarkStrategy<ProductEvent> productWatermark =
                JoinDataGenerators.createProductWatermarkStrategy(Duration.ofSeconds(5));

        DataStream<OrderEvent> orders = env
                .fromSource(orderSource, orderWatermark, "Orders")
                .name("orders-source");

        DataStream<CustomerEvent> customers = env
                .fromSource(customerSource, customerWatermark, "Customers")
                .name("customers-source");

        DataStream<ProductEvent> products = env
                .fromSource(productSource, productWatermark, "Products")
                .name("products-source");

        // ================================================================
        // Pattern 1: Keyed CoProcess Join (Orders + Customers)
        // Use when: Both streams are keyed, need to maintain state
        // ================================================================
        System.out.println("\n=== Pattern 1: Keyed CoProcess Join (Orders + Customers) ===");

        SingleOutputStreamOperator<String> orderWithCustomer = orders
                .keyBy(o -> o.customerId)
                .connect(customers.keyBy(c -> c.customerId))
                .process(new OrderCustomerJoinFunction())
                .name("order-customer-join");

        orderWithCustomer.print("ORDER_CUSTOMER");

        // ================================================================
        // Pattern 2: Broadcast State Join (Orders + Products)
        // Use when: One stream is small and needs to be available everywhere
        // ================================================================
        System.out.println("\n=== Pattern 2: Broadcast State Join (Orders + Products) ===");

        // Define broadcast state descriptor
        MapStateDescriptor<String, ProductEvent> productStateDesc =
                new MapStateDescriptor<>(
                        "product-broadcast",
                        Types.STRING,
                        Types.POJO(ProductEvent.class));

        // Create broadcast stream from products
        BroadcastStream<ProductEvent> productBroadcast = products.broadcast(productStateDesc);

        // Connect orders with broadcast products
        SingleOutputStreamOperator<String> orderWithProduct = orders
                .connect(productBroadcast)
                .process(new OrderProductBroadcastJoinFunction(productStateDesc))
                .name("order-product-broadcast-join");

        orderWithProduct.print("ORDER_PRODUCT");

        // ================================================================
        // Pattern 3: Window Join (Orders + Customers within time window)
        // Use when: Events from both streams occur within a time window
        // ================================================================
        System.out.println("\n=== Pattern 3: Window Join (1-minute tumbling) ===");

        DataStream<String> windowJoinResult = orders
                .keyBy(o -> o.customerId)
                .join(customers.keyBy(c -> c.customerId))
                .where(o -> o.customerId)
                .equalTo(c -> c.customerId)
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
                .apply((order, customer) -> String.format(
                        "WindowJoin: Order %s by %s (%s tier) - $%.2f",
                        order.orderId, customer.name, customer.tier, order.amount))
                .name("window-join");

        windowJoinResult.print("WINDOW_JOIN");

        env.execute("Multi-Stream Join Exercise");
    }

    /**
     * Keyed CoProcess function for joining orders with customer data.
     *
     * <p>Maintains customer state and enriches orders as they arrive.
     * If customer data is not yet available, buffers the order briefly.
     */
    public static class OrderCustomerJoinFunction
            extends KeyedCoProcessFunction<String, OrderEvent, CustomerEvent, String> {

        // State to hold the latest customer information
        private ValueState<CustomerEvent> customerState;
        // State to buffer orders waiting for customer data
        private MapState<String, OrderEvent> pendingOrders;

        @Override
        public void open(OpenContext openContext) throws Exception {
            customerState = getRuntimeContext().getState(
                    new ValueStateDescriptor<>("customer", CustomerEvent.class));
            pendingOrders = getRuntimeContext().getMapState(
                    new MapStateDescriptor<>("pending-orders", String.class, OrderEvent.class));
        }

        @Override
        public void processElement1(OrderEvent order, Context ctx, Collector<String> out)
                throws Exception {
            CustomerEvent customer = customerState.value();

            if (customer != null) {
                // Customer data available - emit enriched order
                out.collect(String.format(
                        "Enriched: Order %s by %s (%s tier, %s) - $%.2f",
                        order.orderId, customer.name, customer.tier,
                        customer.region, order.amount));
            } else {
                // Buffer order and set timer to check later
                pendingOrders.put(order.orderId, order);
                // Set timer for 30 seconds to process buffered orders
                ctx.timerService().registerEventTimeTimer(
                        ctx.timestamp() + 30000);
            }
        }

        @Override
        public void processElement2(CustomerEvent customer, Context ctx, Collector<String> out)
                throws Exception {
            // Update customer state
            customerState.update(customer);

            // Process any pending orders for this customer
            for (OrderEvent order : pendingOrders.values()) {
                out.collect(String.format(
                        "Enriched (delayed): Order %s by %s (%s tier, %s) - $%.2f",
                        order.orderId, customer.name, customer.tier,
                        customer.region, order.amount));
            }
            pendingOrders.clear();
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out)
                throws Exception {
            // Emit any remaining buffered orders with partial data
            CustomerEvent customer = customerState.value();
            for (OrderEvent order : pendingOrders.values()) {
                if (customer != null) {
                    out.collect(String.format(
                            "Enriched (timer): Order %s by %s (%s tier) - $%.2f",
                            order.orderId, customer.name, customer.tier, order.amount));
                } else {
                    out.collect(String.format(
                            "Partial: Order %s by customer %s (unknown) - $%.2f",
                            order.orderId, order.customerId, order.amount));
                }
            }
            pendingOrders.clear();
        }
    }

    /**
     * Broadcast process function for joining orders with product catalog.
     *
     * <p>Products are broadcast to all parallel instances, allowing
     * efficient lookup without shuffling order data.
     */
    public static class OrderProductBroadcastJoinFunction
            extends BroadcastProcessFunction<OrderEvent, ProductEvent, String> {

        private final MapStateDescriptor<String, ProductEvent> productStateDesc;

        public OrderProductBroadcastJoinFunction(
                MapStateDescriptor<String, ProductEvent> productStateDesc) {
            this.productStateDesc = productStateDesc;
        }

        @Override
        public void processElement(OrderEvent order,
                                   ReadOnlyContext ctx,
                                   Collector<String> out) throws Exception {
            // Look up product from broadcast state
            ProductEvent product = ctx.getBroadcastState(productStateDesc).get(order.productId);

            if (product != null) {
                out.collect(String.format(
                        "Order %s: %dx %s (%s) @ $%.2f each = $%.2f",
                        order.orderId, order.quantity, product.name,
                        product.category, product.price, order.amount));
            } else {
                out.collect(String.format(
                        "Order %s: %dx product %s (catalog pending) = $%.2f",
                        order.orderId, order.quantity, order.productId, order.amount));
            }
        }

        @Override
        public void processBroadcastElement(ProductEvent product,
                                            Context ctx,
                                            Collector<String> out) throws Exception {
            // Update the broadcast state with new product information
            ctx.getBroadcastState(productStateDesc).put(product.productId, product);
        }
    }
}
