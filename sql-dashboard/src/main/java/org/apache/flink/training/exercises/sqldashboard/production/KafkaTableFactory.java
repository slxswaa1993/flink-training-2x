package org.apache.flink.training.exercises.sqldashboard.production;

/**
 * Builds Flink SQL DDL statements for Kafka (Azure Event Hubs) source and sink tables.
 */
public class KafkaTableFactory {

    private final JobConfig config;

    public KafkaTableFactory(JobConfig config) {
        this.config = config;
    }

    /** DDL for the orders source table (reads from orders-raw Event Hub). */
    public String ordersSourceDDL() {
        return "CREATE TABLE orders (\n"
                + "  orderId     STRING,\n"
                + "  customerId  STRING,\n"
                + "  productId   STRING,\n"
                + "  productName STRING,\n"
                + "  category    STRING,\n"
                + "  region      STRING,\n"
                + "  quantity    INT,\n"
                + "  unitPrice   DOUBLE,\n"
                + "  totalAmount DOUBLE,\n"
                + "  orderTime   TIMESTAMP(3),\n"
                + "  status      STRING,\n"
                + "  WATERMARK FOR orderTime AS orderTime - INTERVAL '5' SECOND\n"
                + ") WITH (\n"
                + kafkaWith("orders-raw", "latest-offset")
                + ")";
    }

    /** DDL for orders-by-region sink table. */
    public String ordersByRegionSinkDDL() {
        return "CREATE TABLE orders_by_region_sink (\n"
                + "  region       STRING,\n"
                + "  window_start TIMESTAMP(3),\n"
                + "  window_end   TIMESTAMP(3),\n"
                + "  order_count  BIGINT,\n"
                + "  revenue      DOUBLE\n"
                + ") WITH (\n"
                + kafkaWith("orders-by-region", "latest-offset")
                + ")";
    }

    /** DDL for top-products sink table (upsert-kafka because ROW_NUMBER produces changelog). */
    public String topProductsSinkDDL() {
        return "CREATE TABLE top_products_sink (\n"
                + "  productName   STRING,\n"
                + "  category      STRING,\n"
                + "  window_start  TIMESTAMP(3),\n"
                + "  total_sold    BIGINT,\n"
                + "  total_revenue DOUBLE,\n"
                + "  rank_num      BIGINT,\n"
                + "  PRIMARY KEY (window_start, rank_num) NOT ENFORCED\n"
                + ") WITH (\n"
                + upsertKafkaWith("top-products")
                + ")";
    }

    /** DDL for revenue-by-category sink table. */
    public String revenueByCategorySinkDDL() {
        return "CREATE TABLE revenue_by_category_sink (\n"
                + "  category         STRING,\n"
                + "  window_start     TIMESTAMP(3),\n"
                + "  unique_customers BIGINT,\n"
                + "  orders           BIGINT,\n"
                + "  revenue          DECIMAL(10,2),\n"
                + "  avg_order_value  DECIMAL(10,2)\n"
                + ") WITH (\n"
                + kafkaWith("revenue-by-category", "latest-offset")
                + ")";
    }

    /** DDL for high-value-alerts sink table. */
    public String highValueAlertsSinkDDL() {
        return "CREATE TABLE high_value_alerts_sink (\n"
                + "  orderId     STRING,\n"
                + "  customerId  STRING,\n"
                + "  productName STRING,\n"
                + "  region      STRING,\n"
                + "  totalAmount DOUBLE,\n"
                + "  orderTime   TIMESTAMP(3)\n"
                + ") WITH (\n"
                + kafkaWith("high-value-alerts", "latest-offset")
                + ")";
    }

    private String upsertKafkaWith(String topic) {
        return "  'connector' = 'upsert-kafka',\n"
                + "  'topic' = '" + topic + "',\n"
                + "  'properties.bootstrap.servers' = '" + config.bootstrapServers() + "',\n"
                + "  'properties.security.protocol' = 'SASL_SSL',\n"
                + "  'properties.sasl.mechanism' = 'PLAIN',\n"
                + "  'properties.sasl.jaas.config' = '" + config.saslJaasConfig() + "',\n"
                + "  'properties.enable.idempotence' = 'false',\n"
                + "  'key.format' = 'json',\n"
                + "  'value.format' = 'json',\n"
                + "  'value.json.timestamp-format.standard' = 'ISO-8601'\n";
    }

    private String kafkaWith(String topic, String startupMode) {
        return "  'connector' = 'kafka',\n"
                + "  'topic' = '" + topic + "',\n"
                + "  'properties.bootstrap.servers' = '" + config.bootstrapServers() + "',\n"
                + "  'properties.group.id' = '" + config.consumerGroup + "',\n"
                + "  'properties.security.protocol' = 'SASL_SSL',\n"
                + "  'properties.sasl.mechanism' = 'PLAIN',\n"
                + "  'properties.sasl.jaas.config' = '" + config.saslJaasConfig() + "',\n"
                + "  'properties.enable.idempotence' = 'false',\n"
                + "  'scan.startup.mode' = '" + startupMode + "',\n"
                + "  'format' = 'json',\n"
                + "  'json.timestamp-format.standard' = 'ISO-8601'\n";
    }
}
