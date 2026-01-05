# HOW TO RUN AND TAKE SCREENSHOTS - Quick Start Guide

## ⚠️ IMPORTANT: You are a beginner - Start here!

This guide will walk you through **exactly** what to do, step by step.

## What You Need (Prerequisites)

You need these installed on your computer:
1. **Java 8 or higher** - Download from https://adoptium.net/
2. **Apache Maven** - Download from https://maven.apache.org/download.cgi
3. **Apache Spark 3.3.0** - Download from https://spark.apache.org/downloads.html
4. **Git Bash** (Windows only) - Download from https://git-scm.com/

**If you don't know how to install these**, read `BEGINNERS_GUIDE.md` first!

## Step-by-Step Instructions

### Step 1: Verify Everything is Installed

Open **Git Bash** (Windows) or **Terminal** (Mac/Linux) and type:

```bash
java -version
mvn -version
spark-submit --version
```

All three commands should show version numbers. If any says "not found", go back to installation.

### Step 2: Navigate to the Project

```bash
cd /path/to/SPARK-PROJECT
```

Replace `/path/to/SPARK-PROJECT` with where you saved this project.

### Step 3: Build the Project

```bash
mvn clean install
```

**⏱️ This takes 1-2 minutes**

**📸 SCREENSHOT #1**: When it says "BUILD SUCCESS", take a screenshot!

### Step 4: Run the Simple Test

```bash
./simple_test.sh
```

**⏱️ This takes 2-3 minutes**

**📸 SCREENSHOT #2**: When it says "Test Complete!", take a screenshot of the entire output!

### Step 5: Verify the Results

```bash
ls -R bal.db/
```

**📸 SCREENSHOT #3**: Take a screenshot of this directory structure!

You should see:
```
bal.db/
bal.db/bal_diff:
day=2025-01-02
day=2025-01-03

bal.db/bal_diff/day=2025-01-02:
part-xxxxx.parquet

bal.db/bal_diff/day=2025-01-03:
part-xxxxx.parquet

bal.db/bal_latest:
part-xxxxx.parquet
```

### Step 6: Run Each Feature Individually

#### Feature 1: Daily Integration
```bash
./run_daily_file_integration.sh 2025-01-01 sample-data/dump-2025-01-01.csv
```

**📸 SCREENSHOT #4**: Take screenshot showing "Differences saved" and INSERT/UPDATE/DELETE counts

#### Feature 2: Daily Report
```bash
./run_report.sh
```

**📸 SCREENSHOT #5**: Take screenshot showing the table with department statistics

#### Feature 3: Recompute Dump
```bash
./recompute_and_extract_dump_at_date.sh 2025-01-02 /tmp/my_dump
```

**📸 SCREENSHOT #6**: Take screenshot showing "Recomputed dump saved" message

#### Feature 4: Compute Diff
```bash
./compute_diff_between_files.sh /tmp/simple_test_dump_20250102 /tmp/simple_test_dump_20250103
```

**📸 SCREENSHOT #7**: Take screenshot showing the "Difference Summary"

## Where Are the Results?

After running the test, you will find:

1. **bal.db/bal_diff/** - Contains the daily changes (differences)
   - Each day has its own folder: `day=2025-01-02/`, `day=2025-01-03/`, etc.
   - Inside each folder: Parquet files with INSERT/UPDATE/DELETE records

2. **bal.db/bal_latest/** - Contains the most recent snapshot
   - Parquet files with all current addresses

3. **/tmp/** - Contains recomputed dumps
   - Folders like `simple_test_dump_20250102/`
   - These are full datasets recreated for specific dates

## What to Submit to Your Teacher

### 1. All Screenshots (7 total)
Number them clearly:
- Screenshot 1: Maven build success
- Screenshot 2: Simple test complete
- Screenshot 3: Directory structure
- Screenshot 4: Daily integration output
- Screenshot 5: Daily report table
- Screenshot 6: Recompute dump output
- Screenshot 7: Diff comparison

### 2. The Code (Already Done!)
The entire project folder contains all required files:
- ✅ pom.xml (Maven configuration)
- ✅ src/main/java/fr/esilv/*.java (5 Java classes)
- ✅ All 4 shell scripts (.sh files)
- ✅ Sample data for testing

### 3. Explanation Document
Create a Word document or PDF explaining:

**Section 1: What the project does**
- In your own words, explain that it stores French addresses efficiently
- Mention it saves only changes, not full copies every day
- Say this makes 30-year storage possible

**Section 2: The 4 features**
- Daily integration: processes new files and finds changes
- Daily report: shows statistics about addresses
- Recompute dump: recreates data from any past date
- Compute diff: compares two datasets

**Section 3: How it works technically**
- Uses semicolon (`;`) as delimiter in CSV
- Creates unique ID from multiple address fields
- Compares old vs new data to find INSERT/UPDATE/DELETE
- Stores data in Parquet format (compressed and efficient)
- Partitions by day for fast queries

**Section 4: Results you got**
- Include the 7 screenshots
- Mention how many addresses were processed
- Show the directory structure
- Explain what's in bal_diff and bal_latest

## Common Problems and Solutions

### Problem 1: "spark-submit: command not found"
**Solution**: 
- Close and reopen Git Bash
- Check if Spark bin directory is in PATH
- Try running from `C:\spark\bin\spark-submit` directly

### Problem 2: "mvn: command not found"
**Solution**:
- Check if Maven bin directory is in PATH
- Restart Git Bash after installing Maven

### Problem 3: "JAR file not found"
**Solution**:
- Make sure Step 3 (mvn clean install) completed successfully
- Check that `target/spark-bal-addresses-1.0-SNAPSHOT.jar` exists

### Problem 4: Scripts won't run
**Solution**:
```bash
chmod +x *.sh
```

### Problem 5: Windows path issues
**Solution**: Use forward slashes in Git Bash:
- ✅ Good: `/c/Users/YourName/SPARK-PROJECT`
- ❌ Bad: `C:\Users\YourName\SPARK-PROJECT`

## Testing with Real Data (Advanced)

If you want to test with the real 1.4GB French addresses file:

```bash
# 1. Download the file
wget https://adresse.data.gouv.fr/data/ban/adresses/latest/csv-bal/adresses-france.csv.gz

# 2. Decompress it
gunzip adresses-france.csv.gz

# 3. Create 50 test files from it
./prepare_test_data.sh adresses-france.csv

# 4. Run the full integration test
./test_integration.sh
```

**⚠️ Warning**: This will take 30-60 minutes and use several GB of disk space!

## Quick Checklist Before Submission

- [ ] Java, Maven, and Spark are installed
- [ ] Project builds successfully (mvn clean install)
- [ ] Simple test runs and completes
- [ ] All 7 screenshots taken and numbered
- [ ] Explanation document written
- [ ] All files included in submission

## Need More Help?

Read these files in order:
1. **BEGINNERS_GUIDE.md** - Detailed installation and setup
2. **PROJECT_SUMMARY.md** - Technical details and algorithms
3. **README.md** - Project overview

## Success Criteria

Your project is complete when:
- ✅ `mvn clean install` shows BUILD SUCCESS
- ✅ `./simple_test.sh` shows Test Complete!
- ✅ `bal.db/` directory contains bal_diff and bal_latest folders
- ✅ You have 7 clear screenshots
- ✅ You can explain what each feature does

Good luck! 🎓✨
