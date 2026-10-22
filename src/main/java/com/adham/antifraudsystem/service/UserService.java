package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.AuthDtos.UserAccessRequest;
import com.adham.antifraudsystem.dto.AuthDtos.UserAccessResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserDeletionResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserRegistrationRequest;
import com.adham.antifraudsystem.dto.AuthDtos.UserResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserRoleRequest;
import com.adham.antifraudsystem.model.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserService(JdbcTemplate jdbcTemplate,
                       PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("All fields are required");
        }

        String name = request.name().trim();
        String username = request.username().trim();

        Long userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Long.class
        );

        boolean isFirstUser = userCount != null && userCount == 0;

        UserRole role = isFirstUser
                ? UserRole.ADMINISTRATOR
                : UserRole.MERCHANT;

        String encodedPassword = passwordEncoder.encode(request.password());

        jdbcTemplate.update(
                "INSERT INTO users (name, username, password, enabled) VALUES (?, ?, ?, ?)",
                name, username, encodedPassword, isFirstUser
        );

        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                username, role.getAuthority()
        );

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class,
                username
        );

        return new UserResponse(
                id,
                name,
                username,
                role.name()
        );
    }

    @Transactional
    public UserDeletionResponse deleteUser(String username) {

        String existingUsername = jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE username = ?",
                String.class,
                username
        );

        jdbcTemplate.update(
                "DELETE FROM users WHERE username = ?",
                existingUsername
        );

        return new UserDeletionResponse(
                existingUsername,
                "Deleted successfully!"
        );
    }

    @Transactional
    public UserAccessResponse changeUserAccess(UserAccessRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Invalid request");
        }

        String operation = request.operation();

        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE username = ?",
                String.class,
                request.username()
        );

        String authority = jdbcTemplate.queryForObject(
                "SELECT authority FROM authorities WHERE username = ?",
                String.class,
                username
        );

        if (authority.equals("ROLE_ADMINISTRATOR")
                && operation.equals("LOCK")) {
            throw new IllegalArgumentException(
                    "Administrator cannot be locked"
            );
        }

        boolean enabled = operation.equals("UNLOCK");

        jdbcTemplate.update(
                "UPDATE users SET enabled = ? WHERE username = ?",
                enabled,
                username
        );

        String status = enabled ? "unlocked" : "locked";

        return new UserAccessResponse(
                "User " + username + " " + status + "!"
        );
    }

    @Transactional
    public UserResponse changeUserRole(UserRoleRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Invalid username or role");
        }

        UserResponse user = jdbcTemplate.queryForObject(
                """
                SELECT u.id, u.name, u.username, a.authority
                FROM users u
                JOIN authorities a ON u.username = a.username
                WHERE u.username = ?
                """,
                (rs, rowNum) -> new UserResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("authority").substring(5)
                ),
                request.username().trim()
        );

        assert user != null;
        if (user.role().equals(request.role())) {
            throw new IllegalStateException("Role already assigned");
        }

        jdbcTemplate.update(
                "UPDATE authorities SET authority = ? WHERE username = ?",
                "ROLE_" + request.role(),
                user.username()
        );

        return new UserResponse(
                user.id(),
                user.name(),
                user.username(),
                request.role()
        );
    }

    public List<UserResponse> getAllUsers() {

        return jdbcTemplate.query("""
            SELECT u.id, u.name, u.username, a.authority
            FROM users u
            JOIN authorities a ON u.username = a.username
            ORDER BY u.id ASC
            """,
                (rs, rowNum) -> new UserResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("authority").substring(5)
                )
        );
    }

}
