package org.apache.flink.training.exercises.sqldashboard;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.time.Duration;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Real-Time Dashboard with Flink SQL Exercise (Phase 4.2)
 *
 * <p>This exercise teaches you how to:
 * <ul>
 *   <li>Use the Table API to convert DataStreams to Tables</li>
 *   <li>Write Flink SQL for streaming analytics</li>
 *   <li>Use tumbling and sliding windows in SQL</li>
 *   <li>Perform aggregations and TOP-N queries</li>
 * </ul>
 *
 * <h2>Exercise Goals:</h2>
 * <ol>
 *   <li>Create real-time order metrics by region (tumbling window)</li>
 *   <li>Find top-selling products (sliding window)</li>
 *   <li>Track revenue trends</li>
 *   <li>Detect low inventory patterns</li>
 * </ol>
 *
 * <h2>Key SQL Concepts:</h2>
 * <ul>
 *   <li>TUMBLE - fixed-size, non-overlapping windows</li>
 *   <li>HOP - sliding windows with overlap</li>
 *   <li>GROUP BY - aggregation with window functions</li>
 *   <li>Dynamic tables - continuous query semantics</li>
 * </ul>
 */
public class SqlDashboardExercise {

    public static void main(String[] args) throws Exception {
        // Set up the streaming and table environments
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        env.enableCheckpointing(10000);

        // Create order stream
        DataGeneratorSource<Order> source = OrderGenerator.createSource(10.0);
        WatermarkStrategy<Order> watermarkStrategy =
                OrderGenerator.createWatermarkStrategy(Duration.ofSeconds(5));

        DataStream<Order> orderStream = env
                .fromSource(source, watermarkStrategy, "Orders")
                .name("orders-source");

        // Convert DataStream to Table
        // The Table API automatically infers the schema from the POJO
        Table ordersTable = tableEnv.fromDataStream(
                orderStream,
                $("orderId"),
                $("customerId"),
                $("productId"),
                $("productName"),
                $("category"),
                $("region"),
                $("quantity"),
                $("unitPrice"),
                $("totalAmount"),
                $("orderTime").rowtime(),  // Use orderTime as event time
                $("status"));

        // Register the table for SQL queries
        tableEnv.createTemporaryView("orders", ordersTable);

        // ================================================================
        // Query 1: Orders per minute by region (Tumbling Window)
        // ================================================================
        String ordersByRegionSql = """
            SELECT
                region,
                TUMBLE_START(orderTime, INTERVAL '1' MINUTE) AS window_start,
                TUMBLE_END(orderTime, INTERVAL '1' MINUTE) AS window_end,
                COUNT(*) AS order_count,
                SUM(totalAmount) AS revenue
            FROM orders
            GROUP BY
                region,
                TUMBLE(orderTime, INTERVAL '1' MINUTE)
            """;

        Table ordersByRegion = tableEnv.sqlQuery(ordersByRegionSql);

        System.out.println("\n=== Query 1: Orders by Region (1-minute tumbling window) ===");
        tableEnv.toChangelogStream(ordersByRegion).print("REGION_STATS");

        // ================================================================
        // Query 2: Top 3 selling products (Sliding Window)
        // ================================================================
        String topProductsSql = """
            SELECT *
            FROM (
                SELECT
                    productName,
                    category,
                    HOP_START(orderTime, INTERVAL '30' SECOND, INTERVAL '1' MINUTE) AS window_start,
                    SUM(quantity) AS total_sold,
                    SUM(totalAmount) AS total_revenue,
                    ROW_NUMBER() OVER (
                        PARTITION BY HOP_START(orderTime, INTERVAL '30' SECOND, INTERVAL '1' MINUTE)
                        ORDER BY SUM(quantity) DESC
                    ) AS rank_num
                FROM orders
                GROUP BY
                    productName,
                    category,
                    HOP(orderTime, INTERVAL '30' SECOND, INTERVAL '1' MINUTE)
            )
            WHERE rank_num <= 3
            """;

        Table topProducts = tableEnv.sqlQuery(topProductsSql);

        System.out.println("\n=== Query 2: Top 3 Products (1-minute sliding window, 30s slide) ===");
        tableEnv.toChangelogStream(topProducts).print("TOP_PRODUCTS");

        // ================================================================
        // Query 3: Revenue trends by category
        // ================================================================
        String revenueTrendsSql = """
            SELECT
                category,
                TUMBLE_START(orderTime, INTERVAL '1' MINUTE) AS window_start,
                COUNT(DISTINCT customerId) AS unique_customers,
                COUNT(*) AS orders,
                CAST(SUM(totalAmount) AS DECIMAL(10,2)) AS revenue,
                CAST(AVG(totalAmount) AS DECIMAL(10,2)) AS avg_order_value
            FROM orders
            WHERE status = 'CONFIRMED' OR status = 'DELIVERED'
            GROUP BY
                category,
                TUMBLE(orderTime, INTERVAL '1' MINUTE)
            """;

        Table revenueTrends = tableEnv.sqlQuery(revenueTrendsSql);

        System.out.println("\n=== Query 3: Revenue Trends by Category ===");
        tableEnv.toChangelogStream(revenueTrends).print("REVENUE_TRENDS");

        // ================================================================
        // Query 4: High-value order alerts (pattern detection)
        // ================================================================
        String highValueOrdersSql = """
            SELECT
                orderId,
                customerId,
                productName,
                region,
                totalAmount,
                orderTime
            FROM orders
            WHERE totalAmount > 500
            """;

        Table highValueOrders = tableEnv.sqlQuery(highValueOrdersSql);

        System.out.println("\n=== Query 4: High-Value Order Alerts (>$500) ===");
        tableEnv.toChangelogStream(highValueOrders).print("HIGH_VALUE");

        env.execute("SQL Dashboard Exercise");
    }
}
