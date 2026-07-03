package edu.itc.cloud.web.dto;

import edu.itc.cloud.model.FileEntity;
import edu.itc.cloud.model.Folder;
import edu.itc.cloud.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request / response payloads for the REST API, grouped in one file for brevity. */
public final class Dtos {

    private Dtos() {
    }

    public record RegisterRequest(@Email @NotBlank String email,
                                  @NotBlank String password) {
    }

    public record LoginRequest(@NotBlank String email,
                               @NotBlank String password) {
    }

    public record UpdateProfileRequest(String displayName, String password) {
    }

    public record TokenResponse(String token, Long userId, String email) {
    }

    public record UserResponse(Long id, String email, String displayName,
                               long quotaBytes, long usedBytes, long freeBytes) {
        public static UserResponse from(User u, long free) {
            return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(),
                    u.getQuotaBytes(), u.getUsedBytes(), free);
        }
    }

    public record CreateFolderRequest(@NotBlank String name, Long parentId) {
    }

    public record FolderResponse(Long id, String name, Long parentId) {
        public static FolderResponse from(Folder f) {
            return new FolderResponse(f.getId(), f.getName(), f.getParentId());
        }
    }

    public record FileResponse(Long id, String name, long sizeBytes, Long folderId, String shareLink) {
        public static FileResponse from(FileEntity f, String shareLink) {
            return new FileResponse(f.getId(), f.getName(), f.getSizeBytes(), f.getFolderId(), shareLink);
        }
    }

    public record ErrorResponse(String error, String message) {
    }
}
