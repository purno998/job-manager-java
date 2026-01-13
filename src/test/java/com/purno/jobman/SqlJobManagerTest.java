package com.purno.jobman;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.Statement;

class SqlJobManagerTest extends AbstractJobManagerTest {
    private JdbcDataSource dataSource;

    @Override
    protected JobStore createJobStore() {
        dataSource = new JdbcDataSource();
        // Use a unique name per test to ensure a fresh DB
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        return new SqlJobStore(dataSource);
    }

    @AfterEach
    void cleanUp() throws Exception {
        // Drop the table to clean up between tests
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS jobs");
        }
    }
}
