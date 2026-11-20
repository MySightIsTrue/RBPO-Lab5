package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.TokenService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final TokenService tokens;
    public AuthController(TokenService tokens) { this.tokens = tokens; }

    public record Login(String username, String password, String deviceId) {}
    public record Registration(String username, String email, String password, String deviceId) {}
    public record Refresh(String refreshToken) {}

    @PostMapping("/login")
    public TokenService.Pair login(@RequestBody Login body) {
        return tokens.login(body.username(), body.password(), body.deviceId());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenService.Pair register(@RequestBody Registration body) {
        User user = new User();
        user.setUsername(body.username());
        user.setEmail(body.email());
        user.setPassword(body.password());
        return tokens.register(user, body.deviceId());
    }

    @PostMapping("/refresh")
    public TokenService.Pair refresh(@RequestBody Refresh body) {
        var pair = tokens.refresh(body.refreshToken());
        if (pair == null) throw TokenService.denied();
        return pair;
    }
}
