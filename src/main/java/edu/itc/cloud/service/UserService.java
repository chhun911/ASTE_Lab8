package edu.itc.cloud.service;

import edu.itc.cloud.model.User;
import edu.itc.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account lifecycle: register, login, update profile, delete own account. */
@Service
public class UserService {

    /** Anchored pattern used both here and in the API/regex tests. */
    public static final String EMAIL_PATTERN = "^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$";

    private final UserRepository users;
    private final StorageService storage;
    private final long defaultQuotaBytes;

    public UserService(UserRepository users,
                       StorageService storage,
                       @Value("${app.storage.default-quota-bytes:52428800}") long defaultQuotaBytes) {
        this.users = users;
        this.storage = storage;
        this.defaultQuotaBytes = defaultQuotaBytes;
    }

    /** Register a new user. The new account is granted the default quota (50 MB). */
    @Transactional
    public User register(String email, String rawPassword) {
        if (email == null || !email.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        String displayName = email.substring(0, email.indexOf('@'));
        User user = new User(email, displayName, hash(rawPassword), defaultQuotaBytes);
        return users.save(user);
    }

    /** Authenticate by email + password; returns the user on success. */
    @Transactional(readOnly = true)
    public User login(String email, String rawPassword) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Invalid credentials"));
        if (!user.getPasswordHash().equals(hash(rawPassword))) {
            throw new AccessDeniedException("Invalid credentials");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User require(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("No such user: " + userId));
    }

    /** Update the display name and/or password. Blank fields are left unchanged. */
    @Transactional
    public User updateProfile(Long userId, String displayName, String newRawPassword) {
        User user = require(userId);
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName);
        }
        if (newRawPassword != null && !newRawPassword.isBlank()) {
            user.setPasswordHash(hash(newRawPassword));
        }
        return users.save(user);
    }

    /** Delete the user's own account together with all of their folders and files. */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = require(userId);
        storage.deleteAllFor(user);
        users.delete(user);
    }

    /**
     * Deliberately minimal password hash for the lab. In production use BCrypt
     * (spring-security-crypto) and wire in Spring Security instead.
     */
    private String hash(String raw) {
        return Integer.toHexString((raw == null ? "" : raw).hashCode());
    }
}
