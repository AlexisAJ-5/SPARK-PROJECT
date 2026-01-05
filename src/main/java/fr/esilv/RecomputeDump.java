package fr.esilv;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

/**
 * Recompute dump job.
 * Reconstructs the full dataset as of a specific date by applying all changes up to that date.
 */
public class RecomputeDump {
    
    private static final String WAREHOUSE_DIR = "bal.db";
    private static final String DIFF_TABLE = "bal_diff";
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: recompute-dump <date> <outputDir>");
            System.exit(1);
        }
        
        String targetDate = args[0];
        String outputDir = args[1];
        
        System.out.println("Recomputing dump for date: " + targetDate);
        System.out.println("Output directory: " + outputDir);
        
        SparkSession spark = SparkSession.builder()
                .appName("BAL Recompute Dump - " + targetDate)
                .config("spark.sql.warehouse.dir", WAREHOUSE_DIR)
                .enableHiveSupport()
                .getOrCreate();
        
        try {
            String diffPath = WAREHOUSE_DIR + "/" + DIFF_TABLE;
            
            // Read all diff partitions up to target date
            Dataset<Row> allDiffs = spark.read()
                .parquet(diffPath)
                .filter(col("day").leq(lit(targetDate)))
                .orderBy("day");
            
            // Show summary of changes
            System.out.println("\n=== Change summary up to " + targetDate + " ===");
            allDiffs.groupBy("day", "change_type")
                .count()
                .orderBy("day", "change_type")
                .show(200, false);
            
            // Reconstruct the state by processing changes in order
            Dataset<Row> result = reconstructState(allDiffs);
            
            // Save result
            result.write()
                .mode(SaveMode.Overwrite)
                .parquet(outputDir);
            
            long count = result.count();
            System.out.println("\nRecomputed dump saved to: " + outputDir);
            System.out.println("Total addresses in dump: " + count);
            
        } catch (Exception e) {
            System.err.println("Error recomputing dump: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            spark.stop();
        }
    }
    
    private static Dataset<Row> reconstructState(Dataset<Row> allDiffs) {
        System.out.println("Reconstructing state from diffs...");
        
        // Get latest state for each address_id by processing changes chronologically
        // We need to track the latest change type for each address
        
        // Add row number partitioned by address_id, ordered by day descending
        Dataset<Row> rankedDiffs = allDiffs
            .withColumn("rn", 
                row_number().over(
                    org.apache.spark.sql.expressions.Window
                        .partitionBy("address_id")
                        .orderBy(col("day").desc())
                )
            );
        
        // Keep only the latest change for each address_id
        Dataset<Row> latestChanges = rankedDiffs.filter(col("rn").equalTo(1))
            .drop("rn");
        
        // Filter out deleted addresses and keep only INSERT and UPDATE
        Dataset<Row> activeAddresses = latestChanges
            .filter(col("change_type").notEqual(lit("DELETE")))
            .drop("change_type", "day");
        
        return activeAddresses;
    }
}
