#!/bin/bash

# prepare_test_data.sh
# Prepares test data by creating sample CSV files from a single source CSV
# This simulates having 50 daily dumps

if [ $# -lt 1 ]; then
    echo "Usage: $0 <source_csv_file>"
    echo "Example: $0 /path/to/adresses-france.csv"
    exit 1
fi

SOURCE_CSV=$1
TEST_DATA_DIR="test-data"

echo "=== Preparing Test Data ==="
echo "Source CSV: $SOURCE_CSV"
echo "Test data directory: $TEST_DATA_DIR"

# Check if source file exists
if [ ! -f "$SOURCE_CSV" ]; then
    echo "Error: Source CSV file not found: $SOURCE_CSV"
    exit 1
fi

# Create test data directory
mkdir -p "$TEST_DATA_DIR"

# If source is gzipped, decompress it first
if [[ "$SOURCE_CSV" == *.gz ]]; then
    echo "Decompressing gzipped file..."
    TEMP_CSV="$TEST_DATA_DIR/temp_source.csv"
    gunzip -c "$SOURCE_CSV" > "$TEMP_CSV"
    SOURCE_CSV="$TEMP_CSV"
fi

# Count total lines
TOTAL_LINES=$(wc -l < "$SOURCE_CSV")
echo "Total lines in source: $TOTAL_LINES"

# Get header
HEADER=$(head -n 1 "$SOURCE_CSV")

# Calculate lines per file (excluding header)
DATA_LINES=$((TOTAL_LINES - 1))
LINES_PER_FILE=$((DATA_LINES / 50))
echo "Lines per test file: $LINES_PER_FILE"

# Create 50 daily dumps
echo "Creating 50 daily CSV files..."
for n in $(seq 0 49); do
    DAY=$(date -d "2025-01-01 +${n} day" +%Y-%m-%d 2>/dev/null || date -v+${n}d -j -f "%Y-%m-%d" "2025-01-01" +%Y-%m-%d 2>/dev/null)
    OUTPUT_FILE="$TEST_DATA_DIR/dump-${DAY}.csv"
    
    # Calculate line range
    START_LINE=$((n * LINES_PER_FILE + 2))  # +2 to skip header line
    END_LINE=$(((n + 1) * LINES_PER_FILE + 1))
    
    # Special handling for last file to include all remaining lines
    if [ $n -eq 49 ]; then
        END_LINE=$TOTAL_LINES
    fi
    
    # Create file with header
    echo "$HEADER" > "$OUTPUT_FILE"
    
    # Add data lines
    sed -n "${START_LINE},${END_LINE}p" "$SOURCE_CSV" >> "$OUTPUT_FILE"
    
    # Add some random variations to simulate changes (except for first file)
    if [ $n -gt 0 ]; then
        # Randomly skip 1-5 lines to simulate deletions
        NUM_TO_SKIP=$((RANDOM % 5 + 1))
        TEMP_FILE="$TEST_DATA_DIR/temp_${DAY}.csv"
        
        head -n 1 "$OUTPUT_FILE" > "$TEMP_FILE"
        tail -n +2 "$OUTPUT_FILE" | shuf | tail -n +$((NUM_TO_SKIP + 1)) >> "$TEMP_FILE"
        mv "$TEMP_FILE" "$OUTPUT_FILE"
    fi
    
    LINE_COUNT=$(wc -l < "$OUTPUT_FILE")
    echo "Created: $OUTPUT_FILE ($LINE_COUNT lines)"
done

# Clean up temp file if created
if [ -f "$TEST_DATA_DIR/temp_source.csv" ]; then
    rm "$TEST_DATA_DIR/temp_source.csv"
fi

echo ""
echo "Test data preparation complete!"
echo "Test files created in: $TEST_DATA_DIR/"
echo ""
echo "To run the integration test:"
echo "  ./test_integration.sh"
