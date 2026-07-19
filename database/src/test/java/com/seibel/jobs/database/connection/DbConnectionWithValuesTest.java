package com.seibel.jobs.database.connection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootTest()
@ActiveProfiles("test-database")
@TestPropertySource(properties = {
        "CPSS_USERNAME=[username]",
        "CPSS_PASSWORD=[password]"
})
class DbConnectionWithValuesTest {
    @Autowired
    DataSource dataSource;

//    @Test
    void testConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("✅ Connected to DB: " + conn.getMetaData().getURL());
        }
    }
}
