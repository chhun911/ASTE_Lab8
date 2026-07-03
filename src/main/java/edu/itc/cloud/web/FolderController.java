package edu.itc.cloud.web;

import edu.itc.cloud.model.User;
import edu.itc.cloud.service.StorageService;
import edu.itc.cloud.web.dto.Dtos.CreateFolderRequest;
import edu.itc.cloud.web.dto.Dtos.FolderResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final StorageService storage;
    private final AuthService auth;

    public FolderController(StorageService storage, AuthService auth) {
        this.storage = storage;
        this.auth = auth;
    }

    @PostMapping
    public ResponseEntity<FolderResponse> create(@RequestHeader(value = "Authorization", required = false) String authz,
                                                 @Valid @RequestBody CreateFolderRequest req) {
        User user = auth.currentUser(authz);
        FolderResponse body = FolderResponse.from(storage.createFolder(user, req.name(), req.parentId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<FolderResponse> list(@RequestHeader(value = "Authorization", required = false) String authz,
                                     @RequestParam(value = "parent", required = false) Long parentId) {
        User user = auth.currentUser(authz);
        return storage.listFolders(user, parentId).stream().map(FolderResponse::from).toList();
    }

    @PatchMapping("/{id}")
    public FolderResponse rename(@RequestHeader(value = "Authorization", required = false) String authz,
                                 @PathVariable Long id,
                                 @RequestParam String name) {
        User user = auth.currentUser(authz);
        return FolderResponse.from(storage.renameFolder(user, id, name));
    }

    @PatchMapping("/{id}/move")
    public FolderResponse move(@RequestHeader(value = "Authorization", required = false) String authz,
                               @PathVariable Long id,
                               @RequestParam(value = "parent", required = false) Long parentId) {
        User user = auth.currentUser(authz);
        return FolderResponse.from(storage.moveFolder(user, id, parentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authz,
                                       @PathVariable Long id) {
        User user = auth.currentUser(authz);
        storage.deleteFolder(user, id);
        return ResponseEntity.noContent().build();
    }
}
