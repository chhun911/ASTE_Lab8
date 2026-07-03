package edu.itc.cloud.web;

import edu.itc.cloud.model.FileEntity;
import edu.itc.cloud.model.User;
import edu.itc.cloud.service.StorageService;
import edu.itc.cloud.web.dto.Dtos.FileResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storage;
    private final AuthService auth;

    public FileController(StorageService storage, AuthService auth) {
        this.storage = storage;
        this.auth = auth;
    }

    /** Upload a file (multipart). Rejected with 413 if it would exceed the quota. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> upload(@RequestHeader(value = "Authorization", required = false) String authz,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "folder", required = false) Long folderId)
            throws IOException {
        User user = auth.currentUser(authz);
        FileEntity saved = storage.upload(user, folderId, file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FileResponse.from(saved, storage.createShareLink(saved.getId())));
    }

    @GetMapping
    public List<FileResponse> list(@RequestHeader(value = "Authorization", required = false) String authz,
                                   @RequestParam(value = "folder", required = false) Long folderId) {
        User user = auth.currentUser(authz);
        return storage.listFiles(user, folderId).stream()
                .map(f -> FileResponse.from(f, storage.createShareLink(f.getId())))
                .toList();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@RequestHeader(value = "Authorization", required = false) String authz,
                                           @PathVariable Long id) {
        User user = auth.currentUser(authz);
        byte[] bytes = storage.download(user, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PatchMapping("/{id}")
    public FileResponse rename(@RequestHeader(value = "Authorization", required = false) String authz,
                               @PathVariable Long id,
                               @RequestParam String name) {
        User user = auth.currentUser(authz);
        FileEntity renamed = storage.rename(user, id, name);
        return FileResponse.from(renamed, storage.createShareLink(renamed.getId()));
    }

    @PatchMapping("/{id}/move")
    public FileResponse move(@RequestHeader(value = "Authorization", required = false) String authz,
                             @PathVariable Long id,
                             @RequestParam(value = "folder", required = false) Long folderId) {
        User user = auth.currentUser(authz);
        FileEntity moved = storage.moveFile(user, id, folderId);
        return FileResponse.from(moved, storage.createShareLink(moved.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authz,
                                       @PathVariable Long id) {
        User user = auth.currentUser(authz);
        storage.deleteFile(user, id);
        return ResponseEntity.noContent().build();
    }
}
