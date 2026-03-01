package org.apache.flink.training.exercises.sessionanalytics;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Session Window Analytics Exercise (Phase 3.2)
 *
 * <p>This exercise teaches you how to:
 * <ul>
 *   <li>Use session windows with event time</li>
 *   <li>Handle late-arriving events with side outputs</li>
 *   <li>Configure allowed lateness</li>
 *   <li>Aggregate events within a session</li>
 * </ul>
 *
 * <h2>Exercise Goals:</h2>
 * <ol>
 *   <li>Define sessions with 30-minute inactivity gap</li>
 *   <li>Calculate per-session metrics (page views, duration, cart value)</li>
 *   <li>Handle late-arriving events with side outputs</li>
 *   <li>Implement allowed lateness and output late data separately</li>
 * </ol>
 *
 * <h2>Key Concepts:</h2>
 * <ul>
 *   <li>Session windows - dynamic windows based on activity gaps</li>
 *   <li>Watermarks - tracking event time progress</li>
 *   <li>Allowed lateness - accepting late events for window updates</li>
 *   <li>Side outputs - handling events that arrive too late</li>
 * </ul>
 */
public class SessionAnalyticsExercise {

    private static final Logger LOG = LoggerFactory.getLogger(SessionAnalyticsExercise.class);

    // OutputTag for late events that arrive after allowed lateness
    public static final OutputTag<UserEvent> LATE_EVENTS_TAG =
            new OutputTag<UserEvent>("late-events") {};

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Note: Checkpointing is configured via Flink config in production
        // For demo purposes, we skip enabling it here to avoid directory issues

        // Set parallelism to 1 for clearer output ordering (can be overridden by Flink config)
        env.setParallelism(1);

        // Create the user event source (10 events/sec - with 3 users, each gets ~3/sec)
        // Sessions close after 2 seconds of no activity per user
        DataGeneratorSource<UserEvent> source = UserEventGenerator.createSource(10.0);

        // Use simple watermark strategy (we'll use processing time windows)
        WatermarkStrategy<UserEvent> watermarkStrategy = WatermarkStrategy
                .<UserEvent>forBoundedOutOfOrderness(Duration.ofSeconds(2))
                .withTimestampAssigner((event, timestamp) -> event.getEventTimeMillis());

        DataStream<UserEvent> events = env
                .fromSource(source, watermarkStrategy, "User Events")
                .name("user-events-source");

        // Log raw events (visible in Flink Web UI TaskManager logs)
        events.map(new RawEventLogger())
                .name("log-raw-events");

        // Use PROCESSING TIME session windows for demo (fires based on wall clock)
        // This ensures windows close after 2 seconds of inactivity per user
        // In production with event time, you'd use EventTimeSessionWindows
        SingleOutputStreamOperator<SessionStatistics> sessionStats = events
                .keyBy(event -> event.userId)
                .window(EventTimeSessionWindows.withGap(Duration.ofSeconds(2)))
                .allowedLateness(Duration.ofSeconds(2))
                .sideOutputLateData(LATE_EVENTS_TAG)
                .process(new SessionWindowFunction());

        // Log session statistics (visible in Flink Web UI TaskManager logs)
        sessionStats
                .name("session-statistics")
                .map(new SessionStatsLogger())
                .name("log-session-stats");

        LOG.info("Starting Session Analytics - watch TaskManager logs for RAW and SESSION output...");
        env.execute("Session Analytics Exercise");
    }

    /**
     * Logs raw UserEvent to SLF4J (visible in Flink Web UI TaskManager Logs).
     */
    public static class RawEventLogger extends RichMapFunction<UserEvent, UserEvent> {
        private static final long serialVersionUID = 1L;
        private transient Logger log;

        @Override
        public void open(OpenContext openContext) throws Exception {
            log = LoggerFactory.getLogger(RawEventLogger.class);
        }

        @Override
        public UserEvent map(UserEvent event) {
            log.info("[RAW] {} - {}", event.userId, event.eventType);
            return event;
        }
    }

    /**
     * Logs SessionStatistics to SLF4J (visible in Flink Web UI TaskManager Logs).
     */
    public static class SessionStatsLogger extends RichMapFunction<SessionStatistics, SessionStatistics> {
        private static final long serialVersionUID = 1L;
        private transient Logger log;

        @Override
        public void open(OpenContext openContext) throws Exception {
            log = LoggerFactory.getLogger(SessionStatsLogger.class);
        }

        @Override
        public SessionStatistics map(SessionStatistics stats) {
            log.info("[SESSION] {}", stats);
            return stats;
        }
    }
}
