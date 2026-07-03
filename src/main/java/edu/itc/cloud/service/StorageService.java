package edu.itc.cloud.service;

import edu.itc.cloud.model.FileEntity;
import edu.itc.cloud.model.Folder;
import edu.itc.cloud.model.User;
import edu.itc.cloud.repository.FileRepository;
import edu.itc.cloud.repository.FolderRepository;
import edu.itc.cloud.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Files &amp; folders within a user's own storage.
 *
 * <p>Every mutating method takes the acting {@link User} and verifies ownership
 * of any resource it touches, so a user can never read, move or delete another
 * user's data. That per-user isolation is the core invariant of the app.</p>
 */
@Service
public class StorageService {

    private final FileRepository files;
    private final FolderRepository folders;
    private final UserRepository users;

    public StorageService(FileRepository files, FolderRepository folders, UserRepository users) {
        this.files = files;
        this.folders = folders;
        this.users = users;
    }

    // ----- Folders --------------------------------------------------------

    @Transactional
    public Folder createFolder(User owner, String name, Long parentId) {
        requireName(name, "Folder");
        if (parentId != null) {
            requireFolder(owner, parentId); // the parent must belong to the same user
        }
        return folders.save(new Folder(owner.getId(), name, parentId));
    }

    @Transactional(readOnly = true)
    public List<Folder> listFolders(User owner, Long parentId) {
        return folders.findByOwnerIdAndParentIdOrderByNameAsc(owner.getId(), parentId);
    }

    @Transactional
    public Folder renameFolder(User owner, Long folderId, String newName) {
        requireName(newName, "Folder");
        Folder folder = requireFolder(owner, folderId);
        folder.setName(newName);
        return folders.save(folder);
    }

    /** Move a folder under a new parent (or to the root when {@code newParentId} is null). */
    @Transactional
    public Folder moveFolder(User owner, Long folderId, Long newParentId) {
        Folder folder = requireFolder(owner, folderId);
        if (newParentId != null) {
            if (newParentId.equals(folderId)) {
                throw new IllegalArgumentException("A folder cannot be moved into itself");
            }
            requireFolder(owner, newParentId);
        }
        folder.setParentId(newParentId);
        return folders.save(folder);
    }

    @Transactional
    public void deleteFolder(User owner, Long folderId) {
        Folder folder = requireFolder(owner, folderId);
        // Remove files inside this folder first (reclaiming their space).
        for (FileEntity f : files.findByOwnerIdAndFolderIdOrderByNameAsc(owner.getId(), folderId)) {
            deleteFile(owner, f.getId());
        }
        folders.delete(folder);
    }

    // ----- Files ----------------------------------------------------------

    /** Upload a file, enforcing the quota. Throws {@link QuotaExceededException} if it would not fit. */
    @Transactional
    public FileEntity upload(User owner, Long folderId, String name, byte[] content) {
        requireName(name, "File");
        if (folderId != null) {
            requireFolder(owner, folderId);
        }
        long size = content == null ? 0 : content.length;
        long free = freeBytes(owner);
        if (size > free) {
            throw new QuotaExceededException(size, free);
        }
        FileEntity saved = files.save(
                new FileEntity(owner.getId(), folderId, name, content, Instant.now()));
        adjustUsage(owner, size);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<FileEntity> listFiles(User owner, Long folderId) {
        return files.findByOwnerIdAndFolderIdOrderByNameAsc(owner.getId(), folderId);
    }

    /** Convenience: just the file names in a folder (root when {@code folderId} is null). */
    @Transactional(readOnly = true)
    public List<String> list(User owner, Long folderId) {
        return listFiles(owner, folderId).stream().map(FileEntity::getName).toList();
    }

    @Transactional(readOnly = true)
    public byte[] download(User owner, Long fileId) {
        return requireFile(owner, fileId).getContent();
    }

    @Transactional
    public FileEntity rename(User owner, Long fileId, String newName) {
        requireName(newName, "File");
        FileEntity file = requireFile(owner, fileId);
        file.setName(newName);
        return files.save(file);
    }

    /** Move a file into another folder (or to the root when {@code newFolderId} is null). */
    @Transactional
    public FileEntity moveFile(User owner, Long fileId, Long newFolderId) {
        FileEntity file = requireFile(owner, fileId);
        if (newFolderId != null) {
            requireFolder(owner, newFolderId);
        }
        file.setFolderId(newFolderId);
        return files.save(file);
    }

    @Transactional
    public void deleteFile(User owner, Long fileId) {
        FileEntity file = requireFile(owner, fileId);
        long size = file.getSizeBytes();
        files.delete(file);
        adjustUsage(owner, -size);
    }

    /** Wipe everything a user owns (used when an account is deleted). */
    @Transactional
    public void deleteAllFor(User owner) {
        files.deleteByOwnerId(owner.getId());
        folders.deleteByOwnerId(owner.getId());
    }

    // ----- Quota & share links -------------------------------------------

    @Transactional(readOnly = true)
    public long usedBytes(User owner) {
        return reload(owner).getUsedBytes();
    }

    @Transactional(readOnly = true)
    public long freeBytes(User owner) {
        User fresh = reload(owner);
        return fresh.getQuotaBytes() - fresh.getUsedBytes();
    }

    /** Build a deterministic share link of the form {@code /s/{8 hex chars}}. */
    public String createShareLink(Long fileId) {
        String token = String.format("%08x", (fileId * 2654435761L) & 0xffffffffL);
        return "/s/" + token;
    }

    // ----- helpers --------------------------------------------------------

    private void adjustUsage(User owner, long delta) {
        User fresh = reload(owner);
        fresh.setUsedBytes(Math.max(0, fresh.getUsedBytes() + delta));
        users.save(fresh);
        owner.setUsedBytes(fresh.getUsedBytes()); // keep the caller's copy in sync
    }

    private User reload(User owner) {
        return users.findById(owner.getId())
                .orElseThrow(() -> new NotFoundException("No such user: " + owner.getId()));
    }

    private void requireName(String name, String what) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(what + " name must not be blank");
        }
    }

    private Folder requireFolder(User owner, Long folderId) {
        Folder folder = folders.findById(folderId)
                .orElseThrow(() -> new NotFoundException("No such folder: " + folderId));
        if (!folder.getOwnerId().equals(owner.getId())) {
            throw new AccessDeniedException("Folder " + folderId + " does not belong to you");
        }
        return folder;
    }

    private FileEntity requireFile(User owner, Long fileId) {
        FileEntity file = files.findById(fileId)
                .orElseThrow(() -> new NotFoundException("No such file: " + fileId));
        if (!file.getOwnerId().equals(owner.getId())) {
            throw new AccessDeniedException("File " + fileId + " does not belong to you");
        }
        return file;
    }
}
