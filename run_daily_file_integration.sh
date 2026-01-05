#!/bin/bash

# run_daily_file_integration.sh
# Processes a daily CSV file and saves incremental changes

if [ $# -lt 2 ]; then
    echo "Usage: $0 <date> <csvFile>"
    echo "Example: $0 2025-01-01 /path/to/dump-2025-01-01.csv"
    exit 1
fi

DATE=$1
CSV_FILE=$2

echo "=== Running Daily File Integration ==="
echo "Date: $DATE"
echo "CSV File: $CSV_FILE"

# Check if CSV file exists
if [ ! -f "$CSV_FILE" ]; then
    echo "Error: CSV file not found: $CSV_FILE"
    exit 1
fi

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
    daily-integration "$DATE" "$CSV_FILE"

exit $?
