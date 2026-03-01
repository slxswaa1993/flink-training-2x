package org.apache.flink.training.exercises.sqldashboard.production;

/**
 * Loads configuration from environment variables for production Azure Event Hubs deployment.
 */
public class JobConfig {

    public final String eventHubNamespace;
    public final String eventHubConnString;
    public final String consumerGroup;
    public final String checkpointDir;

    public JobConfig() {
        this.eventHubNamespace = requireEnv("EVENTHUB_NAMESPACE");
        this.eventHubConnString = requireEnv("EVENTHUB_CONN_STRING");
        this.consumerGroup = System.getenv().getOrDefault("EVENTHUB_CONSUMER_GROUP", "$Default");
        this.checkpointDir = System.getenv().getOrDefault(
                "FLINK_CHECKPOINT_DIR",
                "abfs://flink-state@prodeus2ordersstorage.dfs.core.windows.net/checkpoints");
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return value;
    }

    public String bootstrapServers() {
        return eventHubNamespace + ".servicebus.windows.net:9093";
    }

    public String saslJaasConfig() {
        return "org.apache.kafka.common.security.plain.PlainLoginModule required "
                + "username=\"$ConnectionString\" "
                + "password=\"" + eventHubConnString + "\";";
    }
}
