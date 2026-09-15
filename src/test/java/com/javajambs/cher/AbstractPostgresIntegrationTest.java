package com.javajambs.cher;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for tests that need a real Postgres instance. Flyway migrations use
 * Postgres-only syntax (BIGSERIAL etc.), so H2/embedded DBs won't run them.
 *
 * The container is started once in a static initializer (the Testcontainers
 * "singleton container" pattern) and deliberately left running rather than
 * managed by @Testcontainers/@Container, so it is shared across every test
 * class that extends this one instead of being stopped after the first
 * class finishes. Testcontainers' Ryuk reaper cleans it up when the JVM
 * exits.
 *
 * Datasource and Flyway properties are overridden explicitly (rather than
 * relying on Testcontainers' @ServiceConnection) because .env sets
 * SPRING_FLYWAY_URL/USER/PASSWORD, which would otherwise point Flyway at the
 * real local dev database instead of this container.
 */
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
