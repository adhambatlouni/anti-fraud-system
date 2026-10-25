package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.AuthDtos.UserAccessRequest;
import com.adham.antifraudsystem.dto.AuthDtos.UserAccessResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserDeletionResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserRegistrationRequest;
import com.adham.antifraudsystem.dto.AuthDtos.UserResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserRoleRequest;
import com.adham.antifraudsystem.model.UserRole;
import com.adham.antifraudsystem.repository.UserRepository;
import com.adham.antifraudsystem.repository.UserRepository.UserAccount;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("All fields are required");
        }

        String name = request.name().trim();
        String username = request.username().trim();
        String password = passwordEncoder.encode(request.password());

        userRepository.lockRegistration();
        boolean firstUser = userRepository.countUsers() == 0;
        UserRole role = firstUser
                ? UserRole.ADMINISTRATOR
                : UserRole.MERCHANT;

        long id = userRepository.createUser(
                name,
                username,
                password,
                firstUser
        );
        userRepository.createAuthority(username, role.getAuthority());

        return new UserResponse(id, name, username, role.name());
    }

    @Transactional
    public UserDeletionResponse deleteUser(String username) {
        String existingUsername = userRepository.findUsername(username);
        userRepository.deleteUser(existingUsername);

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

        String username = userRepository.findUsername(request.username());
        String authority = userRepository.findAuthority(username);

        if (authority.equals(UserRole.ADMINISTRATOR.getAuthority())
                && request.operation().equals("LOCK")) {
            throw new IllegalArgumentException(
                    "Administrator cannot be locked"
            );
        }

        boolean enabled = request.operation().equals("UNLOCK");
        userRepository.updateEnabled(username, enabled);

        return new UserAccessResponse(
                "User " + username + " "
                        + (enabled ? "unlocked" : "locked") + "!"
        );
    }

    @Transactional
    public UserResponse changeUserRole(UserRoleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Invalid username or role");
        }

        UserAccount user = userRepository.findUser(
                request.username().trim()
        );

        if (user.role().equals(request.role())) {
            throw new IllegalStateException("Role already assigned");
        }

        userRepository.updateAuthority(
                user.username(),
                "ROLE_" + request.role()
        );

        return new UserResponse(
                user.id(),
                user.name(),
                user.username(),
                request.role()
        );
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.id(),
                user.name(),
                user.username(),
                user.role()
        );
    }
}
