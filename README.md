# SPARK BAL Addresses Project

An automated Spark-based solution for incremental storage of French BAL (Base Adresse Locale) addresses with long-term retention (30+ years).

## 🚀 Quick Start for Beginners

**NEW TO THIS PROJECT? Start here:**

1. **QUICKSTART.md** - Step-by-step instructions with screenshots guide
2. **BEGINNERS_GUIDE.md** - Detailed installation and setup for Windows
3. **PROJECT_SUMMARY.md** - Technical details and algorithms

## Project Overview

This project processes the French addresses dataset from https://adresse.data.gouv.fr/data/ban/adresses/latest/csv-bal/adresses-france.csv.gz (~1.4GB compressed CSV) and stores only incremental changes daily, making it efficient for long-term storage.

### Storage Strategy
- Instead of storing ~2GB daily dumps (which would be 21 PB over 30 years)
- We store only INSERT/UPDATE/DELETE differences each day
- Data is stored in Parquet format in a Hive-partitioned table
- Latest snapshot is maintained separately for quick access

## Prerequisites

- Java 8 or higher
- Apache Maven 3.6+
- Apache Spark 3.3.0 (installed and `spark-submit` in PATH)
- Bash shell (Linux/Mac) or Git Bash/WSL (Windows)

## Building the Project

```bash
mvn clean install
```

This will create `target/spark-bal-addresses-1.0-SNAPSHOT.jar`

## Features & Shell Scripts

### 1. Daily File Integration
```bash
./run_daily_file_integration.sh <date> <csvFile>
```

### 2. Daily Report
```bash
./run_report.sh
```

### 3. Recompute Dump at Date
```bash
./recompute_and_extract_dump_at_date.sh <date> <outputDir>
```

### 4. Compute Diff Between Files
```bash
./compute_diff_between_files.sh <parquetDir1> <parquetDir2>
```

## Quick Test

Run the simple test with sample data:

```bash
./simple_test.sh
```

## Documentation

- **QUICKSTART.md** - Get started in 5 minutes
- **BEGINNERS_GUIDE.md** - Complete installation and usage guide
- **PROJECT_SUMMARY.md** - Technical architecture and algorithms

## Project Structure

```
SPARK-PROJECT/
├── pom.xml                                    # Maven build configuration
├── src/main/java/fr/esilv/
│   ├── SparkMain.java                         # Main entry point
│   ├── DailyFileIntegration.java              # Feature 1: Daily integration
│   ├── DailyReport.java                       # Feature 2: Statistics report
│   ├── RecomputeDump.java                     # Feature 3: Recompute dump at date
│   └── ComputeDiff.java                       # Feature 4: Compare datasets
├── run_daily_file_integration.sh              # Shell script for daily integration
├── run_report.sh                              # Shell script for report
├── recompute_and_extract_dump_at_date.sh      # Shell script for recomputing dumps
├── compute_diff_between_files.sh              # Shell script for diff computation
├── test_integration.sh                        # Integration test script (50 days)
├── simple_test.sh                             # Quick test (3 days)
├── prepare_test_data.sh                       # Test data generator
└── sample-data/                               # Sample CSV files for testing
```

## Expected Output Structure

After running the integration:

```
bal.db/
├── bal_diff/                    # Incremental changes (partitioned by day)
│   ├── day=2025-01-02/
│   │   └── part-*.parquet
│   ├── day=2025-01-03/
│   │   └── part-*.parquet
│   └── ...
└── bal_latest/                  # Latest snapshot
    └── part-*.parquet
```

## License

Educational project for ESILV Big Data course.

## Author

Spark BAL Addresses Project - 2025

