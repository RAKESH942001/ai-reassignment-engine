package com.ziprun.reassignment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ZipRunApplicationTests {

    @Test
    @DisplayName("Application context should load and connect to PostgreSQL successfully")
    void contextLoads() {
        // Verifies Spring context, DB connection, Hibernate schema, and seed data execution
    }
}
