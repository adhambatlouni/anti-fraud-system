package com.adham.antifraudsystem.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lockRegistration() {
        jdbcTemplate.execute(
                "LOCK TABLE users IN SHARE ROW EXCLUSIVE MODE"
        );
    }

    public long countUsers() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Long.class
        );
        return count == null ? 0 : count;
    }

    public long createUser(
            String name,
            String username,
            String password,
            boolean enabled) {

        Long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO users (name, username, password, enabled)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                name,
                username,
                password,
                enabled
        );

        if (id == null) {
            throw new IllegalStateException("User registration failed");
        }
        return id;
    }

    public void createAuthority(String username, String authority) {
        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                username,
                authority
        );
    }

    public String findUsername(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE username = ?",
                String.class,
                username
        );
    }

    public void deleteUser(String username) {
        jdbcTemplate.update(
                "DELETE FROM users WHERE username = ?",
                username
        );
    }

    public String findAuthority(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT authority FROM authorities WHERE username = ?",
                String.class,
                username
        );
    }

    public void updateEnabled(String username, boolean enabled) {
        jdbcTemplate.update(
                "UPDATE users SET enabled = ? WHERE username = ?",
                enabled,
                username
        );
    }

    public UserAccount findUser(String username) {
        return jdbcTemplate.queryForObject(
                """
                SELECT u.id, u.name, u.username, a.authority
                FROM users u
                JOIN authorities a ON u.username = a.username
                WHERE u.username = ?
                """,
                (rs, rowNum) -> mapUser(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("authority")
                ),
                username
        );
    }

    public void updateAuthority(String username, String authority) {
        jdbcTemplate.update(
                "UPDATE authorities SET authority = ? WHERE username = ?",
                authority,
                username
        );
    }

    public List<UserAccount> findAll() {
        return jdbcTemplate.query(
                """
                SELECT u.id, u.name, u.username, a.authority
                FROM users u
                JOIN authorities a ON u.username = a.username
                ORDER BY u.id ASC
                """,
                (rs, rowNum) -> mapUser(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("authority")
                )
        );
    }

    private UserAccount mapUser(
            long id,
            String name,
            String username,
            String authority) {

        return new UserAccount(
                id,
                name,
                username,
                authority.substring("ROLE_".length())
        );
    }

    public record UserAccount(
            long id,
            String name,
            String username,
            String role
    ) {
    }
}
