package com.jsb.recebimentos.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {
    private final TotpService service = new TotpService();

    @Test
    void verifiesCurrentAuthenticatorCode() {
        String secret = service.newSecret();
        long step = System.currentTimeMillis() / 1000 / 30;

        assertTrue(service.verify(secret, service.code(secret, step)));
    }

    @Test
    void rejectsMalformedAuthenticatorCodes() {
        assertFalse(service.verify(service.newSecret(), "12345"));
        assertFalse(service.verify(service.newSecret(), "abcdef"));
        assertFalse(service.verify("invalid", "123456"));
    }

    @Test
    void generatesAuthenticatorProvisioningQrCode() {
        String dataUri = service.qrDataUri(service.newSecret(), "financeiro");

        assertTrue(dataUri.startsWith("data:image/png;base64,"));
    }
}
