#!/bin/bash

# run_report.sh
# Generates daily statistics report on the latest BAL data

echo "=== Running Daily Report ==="

# Find the JAR file
JAR_FILE=$(find target -name "spark-bal-addresses-*.jar" | grep -v original | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "Error: JAR file not found. Please run 'mvn clean install' first."
    exit 1
fi

# Run Spark job
spark-submit \
    --class fr.esilv.SparkMain \
    --master local[*] \
    --driver-memory 2g \
    --executor-memory 2g \
    "$JAR_FILE" \
    daily-report

exit $?
