package com.suchika.gateway;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbSeeder {

    public static void seed() {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null) {
            dbUrl = "jdbc:postgresql://localhost:5432/app_db";
        }
        String dbUser = System.getenv("DB_USERNAME");
        if (dbUser == null) {
            dbUser = "app_user";
        }
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null) {
            dbPassword = "local_dev_only";
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            System.out.println("DbSeeder: Connected to database " + dbUrl);

            File seedDir = findSeedDir();
            if (seedDir == null) {
                throw new IllegalStateException("Could not find application/flyway/test-seed directory!");
            }

            // Execute profile seed (Truncates CASCADE, so must run first)
            executeSqlFile(stmt, new File(seedDir, "profile/R__seed_profile_test_data.sql"));
            // Execute wealth seed
            executeSqlFile(stmt, new File(seedDir, "wealth/R__seed_wealth_test_data.sql"));
            // Execute health seed
            executeSqlFile(stmt, new File(seedDir, "health/R__seed_health_test_data.sql"));

            System.out.println("DbSeeder: Seeding completed successfully.");

        } catch (Exception e) {
            System.err.println("DbSeeder: Error during seeding: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static File findSeedDir() {
        String[] paths = {
            "application/flyway/test-seed",
            "../flyway/test-seed",
            "../../application/flyway/test-seed",
            "../../../application/flyway/test-seed",
            "../../flyway/test-seed"
        };
        for (String path : paths) {
            File f = new File(path);
            if (f.exists() && f.isDirectory()) {
                return f.getAbsoluteFile();
            }
        }
        return null;
    }

    private static void executeSqlFile(Statement stmt, File file) throws Exception {
        if (!file.exists()) {
            throw new IllegalArgumentException("Seed file does not exist: " + file.getAbsolutePath());
        }
        System.out.println("DbSeeder: Executing " + file.getName());
        String sql = Files.readString(file.toPath());
        stmt.execute(sql);
    }
}
