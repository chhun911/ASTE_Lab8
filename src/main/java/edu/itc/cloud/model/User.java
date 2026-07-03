package edu.itc.cloud.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A registered account. Each user owns an isolated set of folders and files and
 * is granted a fixed storage quota at registration time.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String passwordHash;

    /** Total storage the user is allowed, in bytes. New users receive 50 MB. */
    @Column(nullable = false)
    private long quotaBytes;

    /** Storage currently consumed by the user's files, in bytes. */
    @Column(nullable = false)
    private long usedBytes;

    protected User() {
        // required by JPA
    }

    public User(String email, String displayName, String passwordHash, long quotaBytes) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.quotaBytes = quotaBytes;
        this.usedBytes = 0L;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }
}
