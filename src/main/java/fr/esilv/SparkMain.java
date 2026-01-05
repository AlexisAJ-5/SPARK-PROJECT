package fr.esilv;

import java.util.Arrays;

/**
 * Main entry point for Spark BAL Addresses project.
 * Routes commands to appropriate job classes.
 */
public class SparkMain {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        String[] jobArgs = Arrays.copyOfRange(args, 1, args.length);
        
        try {
            switch (command) {
                case "daily-integration":
                    DailyFileIntegration.main(jobArgs);
                    break;
                case "daily-report":
                    DailyReport.main(jobArgs);
                    break;
                case "recompute-dump":
                    RecomputeDump.main(jobArgs);
                    break;
                case "compute-diff":
                    ComputeDiff.main(jobArgs);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error executing command: " + command);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: spark-submit --class fr.esilv.SparkMain <jar> <command> [args...]");
        System.out.println("\nCommands:");
        System.out.println("  daily-integration <date> <csvFile>     - Process daily CSV file");
        System.out.println("  daily-report                           - Generate daily statistics report");
        System.out.println("  recompute-dump <date> <outputDir>      - Recompute full dump at specific date");
        System.out.println("  compute-diff <parquetDir1> <parquetDir2> - Compare two parquet datasets");
    }
}
