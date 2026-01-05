package fr.esilv;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

/**
 * Daily report job.
 * Generates statistics on the latest BAL data.
 */
public class DailyReport {
    
    private static final String WAREHOUSE_DIR = "bal.db";
    private static final String LATEST_TABLE = "bal_latest";
    
    public static void main(String[] args) {
        System.out.println("=== BAL Daily Report ===");
        
        SparkSession spark = SparkSession.builder()
                .appName("BAL Daily Report")
                .config("spark.sql.warehouse.dir", WAREHOUSE_DIR)
                .enableHiveSupport()
                .getOrCreate();
        
        try {
            String latestPath = WAREHOUSE_DIR + "/" + LATEST_TABLE;
            
            // Check if data exists
            try {
                Dataset<Row> latest = spark.read().parquet(latestPath);
                
                // Total count
                long totalAddresses = latest.count();
                System.out.println("\nTotal number of addresses: " + totalAddresses);
                
                // Extract department code (first 2 digits of code_insee or code_postal)
                Dataset<Row> withDept = latest.withColumn("department", 
                    when(col("code_insee").isNotNull(), 
                        substring(col("code_insee"), 1, 2))
                    .when(col("code_postal").isNotNull(), 
                        substring(col("code_postal"), 1, 2))
                    .otherwise(lit("UNKNOWN"))
                );
                
                // Count by department
                System.out.println("\n=== Number of addresses per department ===");
                Dataset<Row> deptStats = withDept.groupBy("department")
                    .agg(count("*").alias("address_count"))
                    .orderBy(col("department"));
                
                deptStats.show(200, false);
                
                // Additional statistics
                System.out.println("\n=== Top 10 communes by address count ===");
                latest.groupBy("nom_commune", "code_insee")
                    .agg(count("*").alias("address_count"))
                    .orderBy(col("address_count").desc())
                    .show(10, false);
                
                System.out.println("\n=== Position type distribution ===");
                latest.groupBy("type_position")
                    .agg(count("*").alias("count"))
                    .orderBy(col("count").desc())
                    .show(false);
                
            } catch (Exception e) {
                System.err.println("No data available yet. Please run daily integration first.");
                System.err.println("Error: " + e.getMessage());
            }
            
        } finally {
            spark.stop();
        }
    }
}
