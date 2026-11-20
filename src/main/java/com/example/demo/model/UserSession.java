package com.example.demo.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id @GeneratedValue
    private UUID id;
    private String username;
    private String deviceId;
    private String accessToken;
    private String refreshToken;
    private Instant accessTokenExpiry;
    private Instant refreshTokenExpiry;
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.ACTIVE;

    protected UserSession() {}

    public UserSession(String username, String deviceId, String accessToken, String refreshToken,
                       Instant accessTokenExpiry, Instant refreshTokenExpiry) {
        this.username = username;
        this.deviceId = deviceId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String getUsername() { return username; }
    public String getDeviceId() { return deviceId; }
    public Instant getRefreshTokenExpiry() { return refreshTokenExpiry; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
}
