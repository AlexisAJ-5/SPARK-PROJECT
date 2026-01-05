#!/bin/bash

# compute_diff_between_files.sh
# Compares two parquet datasets and shows differences

if [ $# -lt 2 ]; then
    echo "Usage: $0 <parquetDir1> <parquetDir2>"
    echo "Example: $0 /tmp/dumpA /tmp/dumpB"
    exit 1
fi

PARQUET_DIR1=$1
PARQUET_DIR2=$2

echo "=== Computing Diff Between Files ==="
echo "Dataset 1: $PARQUET_DIR1"
echo "Dataset 2: $PARQUET_DIR2"

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
    compute-diff "$PARQUET_DIR1" "$PARQUET_DIR2"

exit $?
