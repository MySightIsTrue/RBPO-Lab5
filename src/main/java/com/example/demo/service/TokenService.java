package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service
public class TokenService {
    public record Pair(String accessToken, String refreshToken) {}

    private final UserService accounts;
    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public TokenService(UserService accounts, UserRepository users, UserSessionRepository sessions,
                        PasswordEncoder passwords, JwtService jwt) {
        this.accounts = accounts;
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    @Transactional
    public Pair login(String username, String password, String deviceId) {
        Rules.text(username, "username");
        Rules.text(password, "password");
        User user = users.findByUsername(username).filter(User::isEnabled).orElseThrow(TokenService::denied);
        if (user.getPasswordHash() == null || !passwords.matches(password, user.getPasswordHash())) throw denied();
        return issue(user, deviceId);
    }

    @Transactional
    public Pair register(User user, String deviceId) {
        return issue(accounts.create(user), deviceId);
    }

    @Transactional
    public Pair refresh(String token) {
        Rules.text(token, "refreshToken");
        Long userId;
        try {
            userId = jwt.verifyRefresh(token);
        } catch (JwtException e) {
            throw denied();
        }
        UserSession session = sessions.findByRefreshToken(token).orElseThrow(TokenService::denied);
        User user = users.findById(userId).filter(User::isEnabled).orElseThrow(TokenService::denied);
        if (!user.getUsername().equals(session.getUsername()) || !session.getRefreshTokenExpiry().isAfter(Instant.now()))
            throw denied();
        if (session.getStatus() == SessionStatus.USED) {
            session.setStatus(SessionStatus.REVOKED);
            return null; // The controller sends 401 after the transaction commits.
        }
        if (session.getStatus() != SessionStatus.ACTIVE) throw denied();
        session.setStatus(SessionStatus.USED);
        return issue(user, session.getDeviceId());
    }

    private Pair issue(User user, String deviceId) {
        if (deviceId != null && deviceId.length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is too long");
        var access = jwt.createAccess(user.getId());
        var refresh = jwt.createRefresh(user.getId());
        sessions.saveAndFlush(new UserSession(user.getUsername(), deviceId, access.value(), refresh.value(),
                access.expiry(), refresh.expiry()));
        return new Pair(access.value(), refresh.value());
    }

    public static ResponseStatusException denied() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }
}
