package com.adham.antifraudsystem.integration;

import com.adham.antifraudsystem.dto.AuthDtos.UserRegistrationRequest;
import com.adham.antifraudsystem.dto.AuthDtos.UserResponse;
import com.adham.antifraudsystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class UserRegistrationConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetUsers() {
        jdbcTemplate.execute("TRUNCATE TABLE audit_events RESTART IDENTITY");
        jdbcTemplate.update("DELETE FROM authorities");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @Timeout(20)
    void createsOnlyOneAdministratorWhenFirstRegistrationsRace()
            throws Exception {

        List<UserResponse> users = runConcurrently(
                new UserRegistrationRequest(
                        "First User",
                        "first-user",
                        "secure-password"
                ),
                new UserRegistrationRequest(
                        "Second User",
                        "second-user",
                        "secure-password"
                )
        );

        assertThat(users)
                .extracting(UserResponse::role)
                .containsExactlyInAnyOrder("ADMINISTRATOR", "MERCHANT");

        Long administratorCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM authorities
                WHERE authority = 'ROLE_ADMINISTRATOR'
                """,
                Long.class
        );
        Long enabledCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE enabled = TRUE",
                Long.class
        );

        assertThat(administratorCount).isOne();
        assertThat(enabledCount).isOne();
    }

    private List<UserResponse> runConcurrently(
            UserRegistrationRequest first,
            UserRegistrationRequest second) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<UserResponse> firstResult = executor.submit(
                    awaitStart(() -> userService.register(first), ready, start)
            );
            Future<UserResponse> secondResult = executor.submit(
                    awaitStart(() -> userService.register(second), ready, start)
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Callable<UserResponse> awaitStart(
            Callable<UserResponse> action,
            CountDownLatch ready,
            CountDownLatch start) {

        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Concurrent test did not start"
                );
            }
            return action.call();
        };
    }
}
