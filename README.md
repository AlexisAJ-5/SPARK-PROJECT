# SPARK BAL Addresses Project

An automated Apache Spark solution for the incremental storage of the French national address base
(BAL, Base Adresse Locale), with long-term retention of 30+ years.

## Project overview

The project processes the French addresses dataset from
[adresse.data.gouv.fr](https://adresse.data.gouv.fr/data/ban/adresses/latest/csv-bal/adresses-france.csv.gz)
(~1.4 GB compressed CSV) and stores only the incremental changes each day, which keeps long-term
storage small.

### Storage strategy

- Storing a full ~2 GB dump every day would take about **22 TB over 30 years**.
- Instead, we store only the daily **INSERT / UPDATE / DELETE** differences.
- Data is stored as **Parquet** in a **Hive-partitioned** table (one partition per day).
- The latest snapshot is kept separately for fast access.

## Prerequisites

- Java 8 or higher
- Apache Maven 3.6+
- Apache Spark 3.3.0 (`spark-submit` in PATH)
- Bash shell (Linux/Mac) or Git Bash/WSL (Windows)

## Build

```bash
mvn clean install
```

This creates `target/spark-bal-addresses-1.0-SNAPSHOT.jar`.

## Features

| Feature | Command | Java class |
|---|---|---|
| Daily file integration | `./run_daily_file_integration.sh <date> <csvFile>` | `DailyFileIntegration` |
| Daily report | `./run_report.sh` | `DailyReport` |
| Recompute the dump at any past date | `./recompute_and_extract_dump_at_date.sh <date> <outputDir>` | `RecomputeDump` |
| Compute the diff between two datasets | `./compute_diff_between_files.sh <parquetDir1> <parquetDir2>` | `ComputeDiff` |

`SparkMain` is the main entry point; the classes live in `src/main/java/fr/esilv/`.

## Quick test

```bash
./simple_test.sh          # 3 simulated days on the sample data
./test_integration.sh     # 50 simulated days
./prepare_test_data.sh    # generates more sample data
```

## Expected output structure

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

## Documentation

- [QUICKSTART.md](QUICKSTART.md): step-by-step setup
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md): technical architecture and algorithms

## Context

Educational project for the ESILV Big Data course (2025), by Alexis Abou Jaoude.
