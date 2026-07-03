package edu.itc.cloud.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A stored file owned by exactly one user. Bytes are kept in the database as a
 * BLOB so the project stays self-contained; a production app would stream them
 * to object storage or disk instead.
 */
@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    /** Containing folder, or {@code null} for the user's root. */
    private Long folderId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private Instant createdAt;

    @Lob
    @Column(nullable = false)
    private byte[] content;

    protected FileEntity() {
        // required by JPA
    }

    public FileEntity(Long ownerId, Long folderId, String name, byte[] content, Instant createdAt) {
        this.ownerId = ownerId;
        this.folderId = folderId;
        this.name = name;
        this.content = content;
        this.sizeBytes = content == null ? 0 : content.length;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public byte[] getContent() {
        return content;
    }
}
