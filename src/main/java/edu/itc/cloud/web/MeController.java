package edu.itc.cloud.web;

import edu.itc.cloud.model.User;
import edu.itc.cloud.service.StorageService;
import edu.itc.cloud.service.UserService;
import edu.itc.cloud.web.dto.Dtos.UpdateProfileRequest;
import edu.itc.cloud.web.dto.Dtos.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService users;
    private final StorageService storage;
    private final AuthService auth;

    public MeController(UserService users, StorageService storage, AuthService auth) {
        this.users = users;
        this.storage = storage;
        this.auth = auth;
    }

    /** Current profile, including quota usage. */
    @GetMapping
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String authz) {
        User user = auth.currentUser(authz);
        return UserResponse.from(user, storage.freeBytes(user));
    }

    /** Update display name and/or password. */
    @PutMapping
    public UserResponse update(@RequestHeader(value = "Authorization", required = false) String authz,
                               @RequestBody UpdateProfileRequest req) {
        User user = auth.currentUser(authz);
        User updated = users.updateProfile(user.getId(), req.displayName(), req.password());
        return UserResponse.from(updated, storage.freeBytes(updated));
    }

    /** Delete the current user's own account (and all of their data). */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authz) {
        User user = auth.currentUser(authz);
        users.deleteAccount(user.getId());
        auth.revoke(auth.extractToken(authz));
        return ResponseEntity.noContent().build();
    }
}
