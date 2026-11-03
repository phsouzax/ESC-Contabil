package com.jsb.recebimentos.security;

import com.jsb.recebimentos.entity.AppUser;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class RecoveryCodeService {
    private final SecureRandom random = new SecureRandom();

    public List<String> generate() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            String hex = HexFormat.of().formatHex(bytes).toUpperCase();
            codes.add(hex.substring(0, 8) + "-" + hex.substring(8, 16) + "-" + hex.substring(16, 24) + "-" + hex.substring(24));
        }
        return codes;
    }

    public String hashAll(List<String> codes) {
        return codes.stream().map(this::hash).reduce((a, b) -> a + "," + b).orElse("");
    }

    public boolean consume(AppUser user, String suppliedCode) {
        if (suppliedCode == null || suppliedCode.isBlank()) return false;
        String candidate = hash(suppliedCode);
        List<String> stored = new ArrayList<>(List.of(user.getRecoveryCodeHashes().split(",")));
        for (int index = 0; index < stored.size(); index++) {
            if (MessageDigest.isEqual(stored.get(index).getBytes(StandardCharsets.US_ASCII), candidate.getBytes(StandardCharsets.US_ASCII))) {
                stored.remove(index);
                user.setRecoveryCodeHashes(String.join(",", stored));
                return true;
            }
        }
        return false;
    }

    private String hash(String code) {
        try {
            String normalized = code.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível validar o código de recuperação.", exception);
        }
    }
}
