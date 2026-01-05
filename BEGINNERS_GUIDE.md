# Beginner's Complete Guide to Running the BAL Addresses Project

## What This Project Does

This project processes French address data efficiently by storing only the **differences** (changes) between daily snapshots, instead of storing complete files every day. This saves huge amounts of storage over 30 years.

## Prerequisites Installation (Windows)

### 1. Install Java
1. Go to https://adoptium.net/
2. Download "Temurin 11 (LTS)" for Windows
3. Run the installer
4. Check installation: Open Command Prompt and type `java -version`

### 2. Install Maven
1. Go to https://maven.apache.org/download.cgi
2. Download the "Binary zip archive" (apache-maven-3.x.x-bin.zip)
3. Extract to `C:\Program Files\Apache\Maven`
4. Add to PATH:
   - Right-click "This PC" → Properties → Advanced System Settings
   - Click "Environment Variables"
   - Under "System Variables", find "Path" and click "Edit"
   - Click "New" and add: `C:\Program Files\Apache\Maven\bin`
5. Check installation: Open **new** Command Prompt and type `mvn -version`

### 3. Install Apache Spark
1. Go to https://spark.apache.org/downloads.html
2. Choose:
   - Spark release: 3.3.0
   - Package type: "Pre-built for Apache Hadoop 3.3 and later"
3. Download the .tgz file
4. Extract using 7-Zip to `C:\spark`
5. Add to PATH (same way as Maven):
   - Add: `C:\spark\bin`
6. Download winutils.exe:
   - Go to https://github.com/cdarlint/winutils/tree/master/hadoop-3.2.2/bin
   - Download winutils.exe to `C:\hadoop\bin\`
7. Set HADOOP_HOME environment variable:
   - Create new System Variable: `HADOOP_HOME` = `C:\hadoop`
8. Check installation: `spark-submit --version`

### 4. Install Git Bash
1. Go to https://git-scm.com/downloads
2. Download and install Git for Windows
3. This installs Git Bash which you'll use to run the scripts

## Building the Project

### Step 1: Open Git Bash
1. Right-click in the project folder (SPARK-PROJECT)
2. Select "Git Bash Here"

### Step 2: Build with Maven
```bash
mvn clean install
```

**SCREENSHOT 1: Take a screenshot when it says "BUILD SUCCESS"**

This creates the JAR file: `target/spark-bal-addresses-1.0-SNAPSHOT.jar`

## Running the Simple Test

### Step 3: Run the Simple Test
```bash
./simple_test.sh
```

This will:
1. Process 3 days of sample data (2025-01-01, 01-02, 01-03)
2. Generate reports showing address counts
3. Recompute dumps for specific dates
4. Compare two dumps to show differences

**SCREENSHOT 2: Take a screenshot of the final "Test Complete!" output**

### What Happens During the Test

The test processes sample CSV files and creates this structure:

```
bal.db/
├── bal_diff/                    # Changes for each day
│   ├── day=2025-01-02/
│   │   └── part-xxx.parquet     # INSERT/UPDATE/DELETE records
│   └── day=2025-01-03/
│       └── part-xxx.parquet
└── bal_latest/                  # Current snapshot
    └── part-xxx.parquet         # All current addresses
```

## Understanding the 4 Features

### Feature 1: Daily Integration
**Script:** `run_daily_file_integration.sh`

**What it does:**
- Reads a new CSV file
- Compares it with yesterday's data
- Saves only the changes (INSERT/UPDATE/DELETE)

**Command:**
```bash
./run_daily_file_integration.sh 2025-01-01 sample-data/dump-2025-01-01.csv
```

**SCREENSHOT 3: Take a screenshot showing the INSERT/UPDATE/DELETE counts**

### Feature 2: Daily Report
**Script:** `run_report.sh`

**What it does:**
- Shows total address count
- Groups addresses by department (75=Paris, 93=Seine-Saint-Denis, etc.)
- Shows top communes

**Command:**
```bash
./run_report.sh
```

**SCREENSHOT 4: Take a screenshot of the department statistics table**

### Feature 3: Recompute Dump
**Script:** `recompute_and_extract_dump_at_date.sh`

**What it does:**
- Recreates the full dataset as it was on a specific date
- Useful for auditing or analysis

**Command:**
```bash
./recompute_and_extract_dump_at_date.sh 2025-01-02 /tmp/dumpA
```

**SCREENSHOT 5: Take a screenshot showing the recomputed dump summary**

### Feature 4: Compute Diff
**Script:** `compute_diff_between_files.sh`

**What it does:**
- Compares two datasets
- Shows what was added, deleted, or modified

**Command:**
```bash
./compute_diff_between_files.sh /tmp/dumpA /tmp/dumpB
```

**SCREENSHOT 6: Take a screenshot of the difference summary**

## Verifying Results

### Step 4: Check the Directory Structure
```bash
ls -R bal.db/
```

**SCREENSHOT 7: Take a screenshot of this directory listing**

You should see:
- `bal.db/bal_diff/day=2025-01-02/` with parquet files
- `bal.db/bal_diff/day=2025-01-03/` with parquet files
- `bal.db/bal_latest/` with parquet files

### Step 5: View Data with Spark Shell (Optional)
```bash
spark-shell
```

Then in the Spark shell:
```scala
val df = spark.read.parquet("bal.db/bal_latest")
df.printSchema()
df.show(10, false)
df.count()
```

**SCREENSHOT 8: Take a screenshot showing the data**

Type `:quit` to exit Spark shell.

## Working with Real Data

When you have the actual 1.4GB CSV file from:
https://adresse.data.gouv.fr/data/ban/adresses/latest/csv-bal/adresses-france.csv.gz

### Option 1: Use Prepare Test Data Script

```bash
# Decompress the file first
gunzip adresses-france.csv.gz

# Create 50 test files
./prepare_test_data.sh /path/to/adresses-france.csv

# Run full integration test
./test_integration.sh
```

**SCREENSHOT 9: Take screenshot of test completion**

### Option 2: Manual Daily Processing

Process one file at a time:
```bash
./run_daily_file_integration.sh 2025-01-01 /path/to/your-dump.csv
./run_report.sh
```

## Common Issues and Solutions

### Issue 1: "spark-submit: command not found"
**Solution:** 
- Restart Git Bash after adding Spark to PATH
- Or manually set in Git Bash:
  ```bash
  export PATH=$PATH:/c/spark/bin
  ```

### Issue 2: "Java heap space" error
**Solution:** Edit the shell scripts and increase memory:
- Change `--driver-memory 2g` to `--driver-memory 4g`
- Change `--executor-memory 2g` to `--executor-memory 4g`

### Issue 3: Windows path errors
**Solution:** In Git Bash, use forward slashes:
- Good: `/c/data/dump.csv` or `c:/data/dump.csv`
- Bad: `c:\data\dump.csv`

### Issue 4: File not found
**Solution:** Use absolute paths:
```bash
./run_daily_file_integration.sh 2025-01-01 "$(pwd)/sample-data/dump-2025-01-01.csv"
```

### Issue 5: Permission denied
**Solution:** Make scripts executable:
```bash
chmod +x *.sh
```

## Expected Test Results

After running `simple_test.sh`, you should have:

1. **3 diff partitions** in `bal.db/bal_diff/`:
   - day=2025-01-02 (1 INSERT + changes)
   - day=2025-01-03 (changes)

2. **1 latest snapshot** in `bal.db/bal_latest/`

3. **2 recomputed dumps** in `/tmp/`:
   - simple_test_dump_20250102
   - simple_test_dump_20250103

4. **Reports** showing:
   - Department 75 (Paris): ~5 addresses
   - Department 93 (Montreuil): ~3 addresses
   - Department 69 (Lyon): ~2 addresses
   - Department 13 (Marseille): ~2 addresses
   - Department 06 (Nice): ~1 address

## What to Submit to Your Teacher

### Required Screenshots:
1. Maven build success
2. Simple test completion
3. Daily integration showing INSERT/UPDATE/DELETE
4. Report showing department statistics
5. Recompute dump output
6. Diff computation results
7. Directory structure (ls -R bal.db/)
8. Parquet file content (optional)

### Required Files:
- All source code (already in the project)
- This guide
- README.md
- Screenshots (numbered and labeled)

### Explanation Document:
Write a brief explanation (in your own words) covering:

1. **What the project does:**
   - Processes French address data
   - Stores only daily changes, not full dumps
   - Saves storage space for 30-year retention

2. **The 4 features:**
   - Daily integration: processes new files
   - Report: shows statistics
   - Recompute: recreates data at any date
   - Diff: compares two datasets

3. **How CSV parsing works:**
   - Delimiter: semicolon (`;`)
   - Encoding: UTF-8
   - Multi-line support for quoted fields
   - 23 columns including ID, address, coordinates

4. **How differences are computed:**
   - Each address gets a unique ID (composite key)
   - New data compared with previous snapshot
   - Changes classified as INSERT/UPDATE/DELETE
   - Only changes are stored in partitioned table

5. **Storage structure:**
   - `bal_diff/` partitioned by day (Hive partitioning)
   - `bal_latest/` contains current snapshot
   - Parquet format for compression and efficiency

## Tips for Success

1. **Start Simple:** Run `simple_test.sh` first before trying real data
2. **Check Logs:** If something fails, read the error message carefully
3. **Test Incrementally:** Run one feature at a time
4. **Save Screenshots:** Take them immediately when tests succeed
5. **Document Issues:** Note any problems and how you solved them

## Getting Help

If you encounter issues:
1. Check the error message
2. Look in the "Common Issues" section above
3. Verify all prerequisites are installed correctly
4. Try with sample data first before real data
5. Check that all shell scripts are executable

Good luck with your project! 🚀
