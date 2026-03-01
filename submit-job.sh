#!/bin/bash
# Submit a Flink job to the running cluster
# Usage: ./submit-job.sh <job-name> [main-class]
#
# Examples:
#   ./submit-job.sh ride-cleansing
#   ./submit-job.sh rides-and-fares
#   ./submit-job.sh hourly-tips
#   ./submit-job.sh long-ride-alerts

set -e

JOB_NAME=${1:-ride-cleansing}
FLINK_REST_URL=${FLINK_REST_URL:-http://localhost:8082}

# Map job names to main classes
declare -A JOB_CLASSES=(
    ["ride-cleansing"]="org.apache.flink.training.exercises.ridecleansing.RideCleansingSolution"
    ["rides-and-fares"]="org.apache.flink.training.exercises.ridesandfares.RidesAndFaresSolution"
    ["hourly-tips"]="org.apache.flink.training.exercises.hourlytips.HourlyTipsSolution"
    ["long-ride-alerts"]="org.apache.flink.training.exercises.longridealerts.LongRideAlertsSolution"
)

MAIN_CLASS=${2:-${JOB_CLASSES[$JOB_NAME]}}
JAR_FILE="jars/${JOB_NAME}-2.0-SNAPSHOT-all.jar"

if [ -z "$MAIN_CLASS" ]; then
    echo "Error: Unknown job name '$JOB_NAME'"
    echo "Available jobs: ${!JOB_CLASSES[*]}"
    exit 1
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR not found: $JAR_FILE"
    echo "Building..."
    ./gradlew :${JOB_NAME}:shadowJar --no-daemon
    cp ${JOB_NAME}/build/libs/${JOB_NAME}-*-all.jar jars/
fi

echo "Submitting job: $JOB_NAME"
echo "Main class: $MAIN_CLASS"
echo "JAR: $JAR_FILE"
echo ""

# Submit via REST API (upload JAR and run)
JAR_ID=$(curl -s -X POST -H "Expect:" -F "jarfile=@${JAR_FILE}" "${FLINK_REST_URL}/jars/upload" | jq -r '.filename | split("/") | last')

if [ "$JAR_ID" == "null" ] || [ -z "$JAR_ID" ]; then
    echo "Error: Failed to upload JAR"
    exit 1
fi

echo "Uploaded JAR: $JAR_ID"

# Run the job
RESPONSE=$(curl -s -X POST "${FLINK_REST_URL}/jars/${JAR_ID}/run?entry-class=${MAIN_CLASS}")
JOB_ID=$(echo "$RESPONSE" | jq -r '.jobid')

if [ "$JOB_ID" == "null" ] || [ -z "$JOB_ID" ]; then
    echo "Error: Failed to submit job"
    echo "$RESPONSE"
    exit 1
fi

echo "Job submitted successfully!"
echo "Job ID: $JOB_ID"
echo ""
echo "View at: ${FLINK_REST_URL}/#/job/${JOB_ID}"
echo "Logs:    docker logs -f flink-taskmanager"
