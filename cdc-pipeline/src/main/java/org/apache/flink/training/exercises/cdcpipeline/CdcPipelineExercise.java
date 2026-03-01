package org.apache.flink.training.exercises.cdcpipeline;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;

/**
 * CDC Pipeline Exercise (Phase 5.1)
 *
 * <p>This exercise teaches you how to:
 * <ul>
 *   <li>Process Change Data Capture (CDC) events</li>
 *   <li>Maintain state for tracking database records</li>
 *   <li>Handle different operation types (INSERT, UPDATE, DELETE)</li>
 *   <li>Route events to different sinks based on table</li>
 * </ul>
 *
 * <h2>Exercise Goals:</h2>
 * <ol>
 *   <li>Process CDC events from multiple tables</li>
 *   <li>Maintain a stateful view of each record's latest state</li>
 *   <li>Detect and alert on important changes (e.g., order status changes)</li>
 *   <li>Route events to appropriate downstream systems</li>
 * </ol>
 *
 * <h2>Key CDC Concepts:</h2>
 * <ul>
 *   <li>Operation types: INSERT, UPDATE, DELETE, READ (snapshot)</li>
 *   <li>Before/After images in change events</li>
 *   <li>Transaction ordering and exactly-once processing</li>
 *   <li>Changelog semantics vs append-only streams</li>
 * </ul>
 *
 * <h2>Production Notes:</h2>
 * <p>In a real CDC pipeline, you would use:
 * <ul>
 *   <li>Flink CDC connector for MySQL/PostgreSQL/MongoDB</li>
 *   <li>Debezium for broader database support</li>
 *   <li>Kafka as an intermediate buffer</li>
 * </ul>
 */
public class CdcPipelineExercise {

    // Side outputs for routing different table events
    public static final OutputTag<ChangeEvent> CUSTOMER_EVENTS =
            new OutputTag<ChangeEvent>("customer-events") {};
    public static final OutputTag<ChangeEvent> ORDER_EVENTS =
            new OutputTag<ChangeEvent>("order-events") {};
    public static final OutputTag<ChangeEvent> PRODUCT_EVENTS =
            new OutputTag<ChangeEvent>("product-events") {};
    public static final OutputTag<ChangeEvent> INVENTORY_EVENTS =
            new OutputTag<ChangeEvent>("inventory-events") {};

    // Side output for alerts
    public static final OutputTag<String> ALERTS =
            new OutputTag<String>("alerts") {};

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Enable checkpointing for exactly-once semantics
        // Critical for CDC pipelines to avoid duplicate or lost data
        env.enableCheckpointing(10000);

        // Create CDC event source (simulated)
        DataGeneratorSource<ChangeEvent> source = ChangeEventGenerator.createSource(20.0);
        WatermarkStrategy<ChangeEvent> watermarkStrategy =
                ChangeEventGenerator.createWatermarkStrategy(Duration.ofSeconds(5));

        DataStream<ChangeEvent> cdcEvents = env
                .fromSource(source, watermarkStrategy, "CDC Events")
                .name("cdc-events-source");

        // Process CDC events with stateful processing
        SingleOutputStreamOperator<ChangeEvent> processedEvents = cdcEvents
                .keyBy(event -> event.tableName + ":" + event.primaryKey)
                .process(new CdcProcessFunction())
                .name("cdc-processor");

        // Route events to different outputs based on table
        // In production, these would go to different Kafka topics or databases

        processedEvents.getSideOutput(CUSTOMER_EVENTS)
                .print("CUSTOMERS");

        processedEvents.getSideOutput(ORDER_EVENTS)
                .print("ORDERS");

        processedEvents.getSideOutput(PRODUCT_EVENTS)
                .print("PRODUCTS");

        processedEvents.getSideOutput(INVENTORY_EVENTS)
                .print("INVENTORY");

        // Alerts stream for important changes
        processedEvents.getSideOutput(ALERTS)
                .print("ALERT");

        // Main output (all processed events)
        processedEvents.print("ALL_CDC");

        env.execute("CDC Pipeline Exercise");
    }

    /**
     * Stateful function that processes CDC events.
     *
     * <p>Maintains the last known state of each record and generates
     * alerts for important state changes.
     */
    public static class CdcProcessFunction
            extends KeyedProcessFunction<String, ChangeEvent, ChangeEvent> {

        // State to track the last known data for each key
        private ValueState<String> lastDataState;
        // State to track if this is the first event for this key
        private ValueState<Boolean> seenBeforeState;

        @Override
        public void open(OpenContext openContext) throws Exception {
            lastDataState = getRuntimeContext().getState(
                    new ValueStateDescriptor<>("lastData", String.class));
            seenBeforeState = getRuntimeContext().getState(
                    new ValueStateDescriptor<>("seenBefore", Boolean.class));
        }

        @Override
        public void processElement(
                ChangeEvent event,
                Context ctx,
                Collector<ChangeEvent> out) throws Exception {

            String lastData = lastDataState.value();
            Boolean seenBefore = seenBeforeState.value();

            // Route to appropriate side output based on table
            OutputTag<ChangeEvent> targetOutput = getOutputTagForTable(event.tableName);
            if (targetOutput != null) {
                ctx.output(targetOutput, event);
            }

            // Generate alerts for important changes
            generateAlerts(event, lastData, seenBefore, ctx);

            // Update state based on operation type
            switch (event.operation) {
                case INSERT:
                case READ:
                case UPDATE:
                    lastDataState.update(event.afterData);
                    seenBeforeState.update(true);
                    break;
                case DELETE:
                    lastDataState.clear();
                    seenBeforeState.clear();
                    break;
            }

            // Emit the processed event
            out.collect(event);
        }

        private OutputTag<ChangeEvent> getOutputTagForTable(String tableName) {
            switch (tableName) {
                case "customers":
                    return CUSTOMER_EVENTS;
                case "orders":
                    return ORDER_EVENTS;
                case "products":
                    return PRODUCT_EVENTS;
                case "inventory":
                    return INVENTORY_EVENTS;
                default:
                    return null;
            }
        }

        private void generateAlerts(
                ChangeEvent event,
                String lastData,
                Boolean seenBefore,
                Context ctx) {

            // Alert on new customers
            if (event.tableName.equals("customers") && event.isInsert()) {
                ctx.output(ALERTS, String.format(
                        "NEW_CUSTOMER: %s at %s", event.primaryKey, event.timestamp));
            }

            // Alert on order status changes
            if (event.tableName.equals("orders") && event.isUpdate()) {
                if (event.afterData != null && event.afterData.contains("\"status\":\"shipped\"")) {
                    ctx.output(ALERTS, String.format(
                            "ORDER_SHIPPED: %s at %s", event.primaryKey, event.timestamp));
                }
            }

            // Alert on low inventory (would parse JSON in production)
            if (event.tableName.equals("inventory") && event.isUpdate()) {
                if (event.afterData != null && event.afterData.contains("\"quantity\":0")) {
                    ctx.output(ALERTS, String.format(
                            "OUT_OF_STOCK: %s at %s", event.primaryKey, event.timestamp));
                }
            }

            // Alert on customer deactivation
            if (event.tableName.equals("customers") && event.isUpdate()) {
                if (event.afterData != null && event.afterData.contains("\"status\":\"inactive\"")) {
                    ctx.output(ALERTS, String.format(
                            "CUSTOMER_DEACTIVATED: %s at %s", event.primaryKey, event.timestamp));
                }
            }

            // Alert on deletes (data integrity concern in some systems)
            if (event.isDelete()) {
                ctx.output(ALERTS, String.format(
                        "RECORD_DELETED: %s.%s at %s",
                        event.tableName, event.primaryKey, event.timestamp));
            }
        }
    }
}
