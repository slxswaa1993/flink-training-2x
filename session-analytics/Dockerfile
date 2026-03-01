# Flink Application Mode Docker Image for Session Analytics
FROM flink:1.20-java17

# Copy the application JAR to the usrlib directory
# Flink application mode looks for JARs in /opt/flink/usrlib/
COPY build/libs/session-analytics-all.jar /opt/flink/usrlib/session-analytics.jar

# Configure logging to write to files that Flink Web UI can read
# Also output to console for docker logs
ENV FLINK_PROPERTIES="jobmanager.rpc.address: jobmanager"

# Set log file location for Web UI access
ENV FLINK_LOG_DIR=/opt/flink/log
ENV FLINK_LOG_FILE=/opt/flink/log/flink.log
