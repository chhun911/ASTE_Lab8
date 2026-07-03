package edu.itc.cloud.web;

import edu.itc.cloud.model.User;
import edu.itc.cloud.service.UserService;
import edu.itc.cloud.web.dto.Dtos.LoginRequest;
import edu.itc.cloud.web.dto.Dtos.RegisterRequest;
import edu.itc.cloud.web.dto.Dtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService users;
    private final AuthService auth;

    public AuthController(UserService users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    /** Register a new user (granted 50 MB) and return a bearer token. */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        User user = users.register(req.email(), req.password());
        String token = auth.issueToken(user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TokenResponse(token, user.getId(), user.getEmail()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.login(req.email(), req.password());
        String token = auth.issueToken(user.getId());
        return new TokenResponse(token, user.getId(), user.getEmail());
    }
}
