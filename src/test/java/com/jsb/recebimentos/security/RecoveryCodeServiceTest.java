package com.jsb.recebimentos.security;

import com.jsb.recebimentos.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryCodeServiceTest {
    private final RecoveryCodeService service = new RecoveryCodeService();

    @Test
    void recoveryCodesAreOneTimeAndStoredAsHashes() {
        List<String> codes = service.generate();
        String hashes = service.hashAll(codes);
        AppUser user = new AppUser("admin", "hash", "encrypted-secret", hashes, LocalDateTime.now());

        assertEquals(10, codes.size());
        assertFalse(user.getRecoveryCodeHashes().contains(codes.get(0)));
        assertTrue(service.consume(user, codes.get(0)));
        assertFalse(service.consume(user, codes.get(0)));
        assertFalse(service.consume(user, "NOT-A-VALID-CODE"));
    }
}
