package com.jsb.recebimentos.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "totp_secret", nullable = false, length = 128)
    private String totpSecret;

    @Column(name = "recovery_code_hashes", nullable = false, columnDefinition = "TEXT")
    private String recoveryCodeHashes;

    @Column(name = "last_mfa_at")
    private LocalDateTime lastMfaAt;

    protected AppUser() { }

    public AppUser(String username, String passwordHash, String totpSecret, String recoveryCodeHashes, LocalDateTime lastMfaAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.totpSecret = totpSecret;
        this.recoveryCodeHashes = recoveryCodeHashes;
        this.lastMfaAt = lastMfaAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getTotpSecret() { return totpSecret; }
    public String getRecoveryCodeHashes() { return recoveryCodeHashes; }
    public LocalDateTime getLastMfaAt() { return lastMfaAt; }
    public void markMfaVerified(LocalDateTime verifiedAt) { this.lastMfaAt = verifiedAt; }
    public void setRecoveryCodeHashes(String hashes) { this.recoveryCodeHashes = hashes; }
}
