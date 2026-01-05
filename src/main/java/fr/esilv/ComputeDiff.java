package fr.esilv;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Compute diff job.
 * Compares two parquet datasets and shows differences.
 */
public class ComputeDiff {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: compute-diff <parquetDir1> <parquetDir2>");
            System.exit(1);
        }
        
        String parquetDir1 = args[0];
        String parquetDir2 = args[1];
        
        System.out.println("Computing differences between:");
        System.out.println("  Dataset 1: " + parquetDir1);
        System.out.println("  Dataset 2: " + parquetDir2);
        
        SparkSession spark = SparkSession.builder()
                .appName("BAL Compute Diff")
                .getOrCreate();
        
        try {
            // Read both datasets
            Dataset<Row> data1 = spark.read().parquet(parquetDir1);
            Dataset<Row> data2 = spark.read().parquet(parquetDir2);
            
            System.out.println("\n=== Dataset Statistics ===");
            long count1 = data1.count();
            long count2 = data2.count();
            System.out.println("Dataset 1 count: " + count1);
            System.out.println("Dataset 2 count: " + count2);
            System.out.println("Difference: " + (count2 - count1));
            
            // Create address_id if not present
            data1 = ensureAddressId(data1);
            data2 = ensureAddressId(data2);
            
            // Find records only in dataset 1 (deleted in dataset 2)
            Dataset<Row> onlyInData1 = data1.join(data2.select("address_id"), 
                    data1.col("address_id").equalTo(data2.col("address_id")), "left_anti");
            long deletedCount = onlyInData1.count();
            
            // Find records only in dataset 2 (added in dataset 2)
            Dataset<Row> onlyInData2 = data2.join(data1.select("address_id"), 
                    data2.col("address_id").equalTo(data1.col("address_id")), "left_anti");
            long addedCount = onlyInData2.count();
            
            // Find common IDs
            Dataset<Row> commonIds = data1.select("address_id")
                .intersect(data2.select("address_id"));
            long commonCount = commonIds.count();
            
            // Find modified records (same ID, different content)
            Dataset<Row> data1Common = data1.join(commonIds, "address_id");
            Dataset<Row> data2Common = data2.join(commonIds, "address_id");
            
            // Get columns to compare (exclude address_id)
            List<String> compareColumns = new ArrayList<>();
            for (String colName : data1.columns()) {
                if (!colName.equals("address_id")) {
                    compareColumns.add(colName);
                }
            }
            
            // Hash rows for comparison
            Dataset<Row> data1Hashed = data1Common.withColumn("row_hash", 
                hash(compareColumns.stream().map(data1Common::col).toArray(org.apache.spark.sql.Column[]::new)));
            Dataset<Row> data2Hashed = data2Common.withColumn("row_hash", 
                hash(compareColumns.stream().map(data2Common::col).toArray(org.apache.spark.sql.Column[]::new)));
            
            Dataset<Row> modified = data1Hashed.join(data2Hashed, 
                    data1Hashed.col("address_id").equalTo(data2Hashed.col("address_id"))
                    .and(data1Hashed.col("row_hash").notEqual(data2Hashed.col("row_hash"))), 
                    "inner")
                .select(data1Hashed.col("address_id"));
            long modifiedCount = modified.count();
            
            // Print summary
            System.out.println("\n=== Difference Summary ===");
            System.out.println("Common addresses: " + commonCount);
            System.out.println("Only in dataset 1 (deleted): " + deletedCount);
            System.out.println("Only in dataset 2 (added): " + addedCount);
            System.out.println("Modified addresses: " + modifiedCount);
            System.out.println("Total changes: " + (deletedCount + addedCount + modifiedCount));
            
            // Show sample of differences
            if (addedCount > 0) {
                System.out.println("\n=== Sample of added addresses (first 10) ===");
                onlyInData2.select("address_id", "nom_commune", "nom_voie", "numero", "code_postal")
                    .show(10, false);
            }
            
            if (deletedCount > 0) {
                System.out.println("\n=== Sample of deleted addresses (first 10) ===");
                onlyInData1.select("address_id", "nom_commune", "nom_voie", "numero", "code_postal")
                    .show(10, false);
            }
            
            if (modifiedCount > 0) {
                System.out.println("\n=== Sample of modified address IDs (first 10) ===");
                modified.show(10, false);
            }
            
        } catch (Exception e) {
            System.err.println("Error computing diff: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            spark.stop();
        }
    }
    
    private static Dataset<Row> ensureAddressId(Dataset<Row> data) {
        // Check if address_id already exists
        boolean hasAddressId = false;
        for (String col : data.columns()) {
            if (col.equals("address_id")) {
                hasAddressId = true;
                break;
            }
        }
        
        if (!hasAddressId) {
            data = data.withColumn("address_id", 
                concat_ws("_", 
                    coalesce(col("id"), lit("")),
                    coalesce(col("numero"), lit("")),
                    coalesce(col("rep"), lit("")),
                    coalesce(col("nom_voie"), lit("")),
                    coalesce(col("code_postal"), lit("")),
                    coalesce(col("code_insee"), lit(""))
                ));
        }
        
        return data;
    }
}
