package fr.esilv;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.*;

/**
 * Daily file integration job.
 * Processes a CSV file, computes differences with previous data, and saves incremental changes.
 */
public class DailyFileIntegration {
    
    private static final String WAREHOUSE_DIR = "bal.db";
    private static final String DIFF_TABLE = "bal_diff";
    private static final String LATEST_TABLE = "bal_latest";
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: daily-integration <date> <csvFile>");
            System.exit(1);
        }
        
        String date = args[0];
        String csvFile = args[1];
        
        System.out.println("Processing daily integration for date: " + date);
        System.out.println("CSV file: " + csvFile);
        
        SparkSession spark = SparkSession.builder()
                .appName("BAL Daily Integration - " + date)
                .config("spark.sql.warehouse.dir", WAREHOUSE_DIR)
                .enableHiveSupport()
                .getOrCreate();
        
        try {
            // Read CSV with proper schema
            Dataset<Row> newData = readBalCsv(spark, csvFile);
            
            // Create unique ID for each address
            newData = newData.withColumn("address_id", 
                concat_ws("_", 
                    coalesce(col("id"), lit("")),
                    coalesce(col("numero"), lit("")),
                    coalesce(col("rep"), lit("")),
                    coalesce(col("nom_voie"), lit("")),
                    coalesce(col("code_postal"), lit("")),
                    coalesce(col("code_insee"), lit(""))
                ));
            
            // Cache for performance
            newData.cache();
            
            String latestPath = WAREHOUSE_DIR + "/" + LATEST_TABLE;
            
            // Check if latest data exists
            boolean latestExists = spark.catalog().tableExists(LATEST_TABLE);
            
            Dataset<Row> diff;
            if (!latestExists) {
                System.out.println("First run - all records are new inserts");
                // First run: all records are inserts
                diff = newData.withColumn("change_type", lit("INSERT"));
            } else {
                // Load previous latest data
                Dataset<Row> oldData = spark.read().parquet(latestPath);
                oldData.cache();
                
                // Compute differences
                diff = computeDifferences(newData, oldData);
                
                oldData.unpersist();
            }
            
            // Add partition column
            diff = diff.withColumn("day", lit(date));
            
            // Save differences to partitioned table
            String diffPath = WAREHOUSE_DIR + "/" + DIFF_TABLE;
            diff.write()
                .mode(SaveMode.Append)
                .partitionBy("day")
                .parquet(diffPath);
            
            // Update latest snapshot
            newData.drop("address_id")
                .write()
                .mode(SaveMode.Overwrite)
                .parquet(latestPath);
            
            // Print statistics
            long totalRecords = newData.count();
            long diffCount = diff.count();
            System.out.println("Total records in new file: " + totalRecords);
            System.out.println("Differences saved: " + diffCount);
            
            diff.groupBy("change_type").count().show();
            
            newData.unpersist();
            
        } catch (Exception e) {
            System.err.println("Error processing daily integration: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            spark.stop();
        }
    }
    
    private static Dataset<Row> readBalCsv(SparkSession spark, String csvFile) {
        // Define schema for BAL CSV
        StructType schema = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("id", DataTypes.StringType, true),
            DataTypes.createStructField("id_fantoir", DataTypes.StringType, true),
            DataTypes.createStructField("numero", DataTypes.StringType, true),
            DataTypes.createStructField("rep", DataTypes.StringType, true),
            DataTypes.createStructField("nom_voie", DataTypes.StringType, true),
            DataTypes.createStructField("code_postal", DataTypes.StringType, true),
            DataTypes.createStructField("code_insee", DataTypes.StringType, true),
            DataTypes.createStructField("nom_commune", DataTypes.StringType, true),
            DataTypes.createStructField("code_insee_ancienne_commune", DataTypes.StringType, true),
            DataTypes.createStructField("nom_ancienne_commune", DataTypes.StringType, true),
            DataTypes.createStructField("x", DataTypes.StringType, true),
            DataTypes.createStructField("y", DataTypes.StringType, true),
            DataTypes.createStructField("lon", DataTypes.StringType, true),
            DataTypes.createStructField("lat", DataTypes.StringType, true),
            DataTypes.createStructField("type_position", DataTypes.StringType, true),
            DataTypes.createStructField("alias", DataTypes.StringType, true),
            DataTypes.createStructField("nom_ld", DataTypes.StringType, true),
            DataTypes.createStructField("libelle_acheminement", DataTypes.StringType, true),
            DataTypes.createStructField("nom_afnor", DataTypes.StringType, true),
            DataTypes.createStructField("source_position", DataTypes.StringType, true),
            DataTypes.createStructField("source_nom_voie", DataTypes.StringType, true),
            DataTypes.createStructField("certification_commune", DataTypes.StringType, true),
            DataTypes.createStructField("cad_parcelles", DataTypes.StringType, true)
        });
        
        return spark.read()
                .option("header", "true")
                .option("delimiter", ";")
                .option("quote", "\"")
                .option("escape", "\"")
                .option("multiLine", "true")
                .option("encoding", "UTF-8")
                .schema(schema)
                .csv(csvFile);
    }
    
    private static Dataset<Row> computeDifferences(Dataset<Row> newData, Dataset<Row> oldData) {
        System.out.println("Computing differences...");
        
        // Add address_id to old data if not present
        if (!columnExists(oldData, "address_id")) {
            oldData = oldData.withColumn("address_id", 
                concat_ws("_", 
                    coalesce(col("id"), lit("")),
                    coalesce(col("numero"), lit("")),
                    coalesce(col("rep"), lit("")),
                    coalesce(col("nom_voie"), lit("")),
                    coalesce(col("code_postal"), lit("")),
                    coalesce(col("code_insee"), lit(""))
                ));
        }
        
        // Find inserted records (in new but not in old)
        Dataset<Row> inserts = newData.join(oldData.select("address_id"), 
                newData.col("address_id").equalTo(oldData.col("address_id")), "left_anti")
            .withColumn("change_type", lit("INSERT"));
        
        // Find deleted records (in old but not in new)
        Dataset<Row> deletes = oldData.join(newData.select("address_id"), 
                oldData.col("address_id").equalTo(newData.col("address_id")), "left_anti")
            .withColumn("change_type", lit("DELETE"));
        
        // Find potentially updated records (same ID but different content)
        Dataset<Row> commonIds = newData.select("address_id")
            .intersect(oldData.select("address_id"));
        
        Dataset<Row> newCommon = newData.join(commonIds, "address_id");
        Dataset<Row> oldCommon = oldData.join(commonIds, "address_id");
        
        // Compare all columns except address_id
        List<String> compareColumns = new ArrayList<>();
        for (String colName : newData.columns()) {
            if (!colName.equals("address_id")) {
                compareColumns.add(colName);
            }
        }
        
        // Detect updates by comparing hash of all columns
        Dataset<Row> newHashed = newCommon.withColumn("row_hash", 
            hash(compareColumns.stream().map(newCommon::col).toArray(org.apache.spark.sql.Column[]::new)));
        Dataset<Row> oldHashed = oldCommon.withColumn("row_hash", 
            hash(compareColumns.stream().map(oldCommon::col).toArray(org.apache.spark.sql.Column[]::new)));
        
        Dataset<Row> updates = newHashed.join(oldHashed, 
                newHashed.col("address_id").equalTo(oldHashed.col("address_id"))
                .and(newHashed.col("row_hash").notEqual(oldHashed.col("row_hash"))), 
                "inner")
            .select(newHashed.col("*"))
            .drop("row_hash")
            .withColumn("change_type", lit("UPDATE"));
        
        // Union all changes
        return inserts.unionByName(deletes).unionByName(updates);
    }
    
    private static boolean columnExists(Dataset<Row> df, String columnName) {
        for (String col : df.columns()) {
            if (col.equals(columnName)) {
                return true;
            }
        }
        return false;
    }
}
