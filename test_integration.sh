#!/bin/bash

# test_integration.sh
# Integration test script that runs all 4 features

set -e  # Exit on error

TEST_DATA_DIR="test-data"
OUTPUT_DIR_A="/tmp/dumpA"
OUTPUT_DIR_B="/tmp/dumpB"

echo "======================================"
echo "   BAL Addresses Integration Test"
echo "======================================"
echo ""

# Check if test data exists
if [ ! -d "$TEST_DATA_DIR" ]; then
    echo "Error: Test data directory not found: $TEST_DATA_DIR"
    echo "Please run ./prepare_test_data.sh first"
    exit 1
fi

# Check if JAR exists
if [ ! -f "target/spark-bal-addresses-1.0-SNAPSHOT.jar" ]; then
    echo "Error: JAR file not found. Running mvn clean install..."
    mvn clean install
fi

# Clean up previous test results
echo "Cleaning up previous test results..."
rm -rf bal.db
rm -rf "$OUTPUT_DIR_A"
rm -rf "$OUTPUT_DIR_B"
rm -rf derby.log metastore_db spark-warehouse

echo ""
echo "======================================"
echo "   Phase 1: Daily Integration Loop"
echo "======================================"
echo ""

# Process files for 50 days
for n in $(seq 0 49); do 
    DAY=$(date -d "2025-01-01 +${n} day" +%Y-%m-%d 2>/dev/null || date -v+${n}d -j -f "%Y-%m-%d" "2025-01-01" +%Y-%m-%d 2>/dev/null)
    CSV_FILE="$TEST_DATA_DIR/dump-${DAY}.csv"
    
    if [ ! -f "$CSV_FILE" ]; then
        echo "Warning: CSV file not found: $CSV_FILE, skipping..."
        continue
    fi
    
    echo ""
    echo "--- Processing day $((n+1))/50: ${DAY} ---"
    ./run_daily_file_integration.sh "${DAY}" "$CSV_FILE"
    
    # Run report every 10 days
    if [ $((n % 10)) -eq 0 ]; then
        echo ""
        echo "--- Running report for day ${DAY} ---"
        ./run_report.sh
    fi
done

echo ""
echo "======================================"
echo "   Phase 2: Final Report"
echo "======================================"
echo ""
./run_report.sh

echo ""
echo "======================================"
echo "   Phase 3: Recompute Dumps"
echo "======================================"
echo ""

echo "Recomputing dump for 2025-01-24..."
./recompute_and_extract_dump_at_date.sh 2025-01-24 "$OUTPUT_DIR_A"

echo ""
echo "Recomputing dump for 2025-02-10..."
./recompute_and_extract_dump_at_date.sh 2025-02-10 "$OUTPUT_DIR_B"

echo ""
echo "======================================"
echo "   Phase 4: Compute Diff"
echo "======================================"
echo ""

./compute_diff_between_files.sh "$OUTPUT_DIR_A" "$OUTPUT_DIR_B"

echo ""
echo "======================================"
echo "   Verification"
echo "======================================"
echo ""

# Check directory structure
echo "Checking directory structure..."
if [ -d "bal.db/bal_diff" ]; then
    PARTITION_COUNT=$(find bal.db/bal_diff -name "day=*" -type d | wc -l)
    echo "✓ bal_diff partitions found: $PARTITION_COUNT"
    echo ""
    echo "Partition structure:"
    find bal.db/bal_diff -name "day=*" -type d | sort | head -10
    if [ $PARTITION_COUNT -gt 10 ]; then
        echo "  ... ($((PARTITION_COUNT - 10)) more partitions)"
    fi
else
    echo "✗ bal_diff directory not found!"
fi

echo ""
if [ -d "bal.db/bal_latest" ]; then
    echo "✓ bal_latest directory found"
    PARQUET_COUNT=$(find bal.db/bal_latest -name "*.parquet" -type f | wc -l)
    echo "  Parquet files: $PARQUET_COUNT"
else
    echo "✗ bal_latest directory not found!"
fi

echo ""
if [ -d "$OUTPUT_DIR_A" ]; then
    echo "✓ Recomputed dump A found"
    PARQUET_COUNT_A=$(find "$OUTPUT_DIR_A" -name "*.parquet" -type f | wc -l)
    echo "  Parquet files: $PARQUET_COUNT_A"
else
    echo "✗ Recomputed dump A not found!"
fi

echo ""
if [ -d "$OUTPUT_DIR_B" ]; then
    echo "✓ Recomputed dump B found"
    PARQUET_COUNT_B=$(find "$OUTPUT_DIR_B" -name "*.parquet" -type f | wc -l)
    echo "  Parquet files: $PARQUET_COUNT_B"
else
    echo "✗ Recomputed dump B not found!"
fi

echo ""
echo "======================================"
echo "   Test Complete!"
echo "======================================"
echo ""
echo "Results are available in:"
echo "  - bal.db/bal_diff/    (incremental changes)"
echo "  - bal.db/bal_latest/  (latest snapshot)"
echo "  - $OUTPUT_DIR_A       (recomputed dump at 2025-01-24)"
echo "  - $OUTPUT_DIR_B       (recomputed dump at 2025-02-10)"
echo ""
echo "Take screenshots of:"
echo "  1. This verification output"
echo "  2. Directory structure: ls -R bal.db/"
echo "  3. Sample parquet content using spark-shell"
