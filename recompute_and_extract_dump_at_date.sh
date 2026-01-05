#!/bin/bash

# recompute_and_extract_dump_at_date.sh
# Recomputes the full dataset as of a specific date

if [ $# -lt 2 ]; then
    echo "Usage: $0 <date> <outputDir>"
    echo "Example: $0 2025-01-24 /tmp/dumpA"
    exit 1
fi

DATE=$1
OUTPUT_DIR=$2

echo "=== Recomputing Dump at Date ==="
echo "Date: $DATE"
echo "Output Directory: $OUTPUT_DIR"

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
    recompute-dump "$DATE" "$OUTPUT_DIR"

exit $?
