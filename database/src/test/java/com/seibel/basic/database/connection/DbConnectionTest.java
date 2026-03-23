package com.seibel.basic.database.connection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootTest()
@ActiveProfiles("test-database")
class DbConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment env; // Spring Environment

    @Test
    void printSpringEnvValues() {
        System.out.println("Spring property CPSS_USERNAME = " + env.getProperty("CPSS_USERNAME"));
        System.out.println("Spring property CPSS_PASSWORD = " + env.getProperty("CPSS_PASSWORD"));
    }

    @Test
    void printEnvValues() {
        System.out.println("System ENV username: " + System.getenv("CPSS_USERNAME"));
        System.out.println("System ENV password: " + System.getenv("CPSS_PASSWORD"));
    }

    @Test
    void testConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("✅ Connected to DB: " + conn.getMetaData().getURL());
        }
    }
}
