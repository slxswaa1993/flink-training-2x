package org.apache.flink.training.exercises.sqldashboard.production;

import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Production Flink SQL job that reads orders from Azure Event Hubs (orders-raw topic)
 * and writes 4 analytics streams to output Event Hub topics.
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>EVENTHUB_NAMESPACE  — Event Hubs namespace (without .servicebus.windows.net)</li>
 *   <li>EVENTHUB_CONN_STRING — Primary connection string (SAS token)</li>
 * </ul>
 */
public class SqlDashboardJob {

    public static void main(String[] args) throws Exception {
        JobConfig config = new JobConfig();
        KafkaTableFactory factory = new KafkaTableFactory(config);

        Configuration flinkConf = new Configuration();
        flinkConf.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, config.checkpointDir);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(flinkConf);
        env.enableCheckpointing(60_000);

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // ── Create source table ──────────────────────────────────────────────
        tableEnv.executeSql(factory.ordersSourceDDL());

        // ── Create sink tables ───────────────────────────────────────────────
        tableEnv.executeSql(factory.ordersByRegionSinkDDL());
        tableEnv.executeSql(factory.topProductsSinkDDL());
        tableEnv.executeSql(factory.revenueByCategorySinkDDL());
        tableEnv.executeSql(factory.highValueAlertsSinkDDL());

        // ── Bundle all INSERT INTO statements into a single job ─────────────
        StatementSet stmtSet = tableEnv.createStatementSet();

        // Query 1: Orders per minute by region (Tumbling Window)
        stmtSet.addInsertSql("""
            INSERT INTO orders_by_region_sink
            SELECT
                region,
                TUMBLE_START(orderTime, INTERVAL '1' MINUTE) AS window_start,
                TUMBLE_END(orderTime, INTERVAL '1' MINUTE)   AS window_end,
                COUNT(*)          AS order_count,
                SUM(totalAmount)  AS revenue
            FROM orders
            GROUP BY
                region,
                TUMBLE(orderTime, INTERVAL '1' MINUTE)
            """);

        // Query 2: Top 3 selling products (Sliding Window)
        stmtSet.addInsertSql("""
            INSERT INTO top_products_sink
            SELECT productName, category, window_start, total_sold, total_revenue, rank_num
            FROM (
                SELECT
                    productName,
                    category,
                    HOP_START(orderTime, INTERVAL '30' SECOND, INTERVAL '1' MINUTE) AS window_start,
                    SUM(quantity)     AS total_sold,
                    SUM(totalAmount)  AS total_revenue,
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
            """);

        // Query 3: Revenue trends by category
        stmtSet.addInsertSql("""
            INSERT INTO revenue_by_category_sink
            SELECT
                category,
                TUMBLE_START(orderTime, INTERVAL '1' MINUTE) AS window_start,
                COUNT(DISTINCT customerId)                   AS unique_customers,
                COUNT(*)                                     AS orders,
                CAST(SUM(totalAmount)  AS DECIMAL(10,2))     AS revenue,
                CAST(AVG(totalAmount)  AS DECIMAL(10,2))     AS avg_order_value
            FROM orders
            WHERE status = 'CONFIRMED' OR status = 'DELIVERED'
            GROUP BY
                category,
                TUMBLE(orderTime, INTERVAL '1' MINUTE)
            """);

        // Query 4: High-value order alerts
        stmtSet.addInsertSql("""
            INSERT INTO high_value_alerts_sink
            SELECT
                orderId,
                customerId,
                productName,
                region,
                totalAmount,
                orderTime
            FROM orders
            WHERE totalAmount > 500
            """);

        stmtSet.execute();
    }
}
