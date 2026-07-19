package com.seibel.jobs.database.connection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@SpringBootTest()
@ActiveProfiles("test-database")
class PrintDatasourceConfigTest {

    @Autowired
    private Environment env;

    @Autowired(required = false)
    private DataSource dataSource;

    @Test
    void printDatasourceConfig() {
        System.out.println("=== DATASOURCE CONFIGURATION ===");
        System.out.println("URL: " + env.getProperty("spring.datasource.url"));
        System.out.println("Username: " + env.getProperty("spring.datasource.username"));
        System.out.println("Password: " + (env.getProperty("spring.datasource.password") != null ? "***SET***" : "NULL"));
        System.out.println("Driver: " + env.getProperty("spring.datasource.driver-class-name"));
        System.out.println("Liquibase enabled: " + env.getProperty("spring.liquibase.enabled"));

        System.out.println("\n=== ENVIRONMENT VARIABLES ===");
        System.out.println("CPSS_USERNAME: " + System.getenv("CPSS_USERNAME"));
        System.out.println("CPSS_PASSWORD: " + (System.getenv("CPSS_PASSWORD") != null ? "***SET***" : "NULL"));

        if (dataSource != null) {
            System.out.println("\n✅ DataSource bean created");
        } else {
            System.out.println("\n❌ DataSource bean NOT created");
        }
    }
}