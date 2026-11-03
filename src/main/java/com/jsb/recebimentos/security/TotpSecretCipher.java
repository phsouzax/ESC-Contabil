package com.jsb.recebimentos.security;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class TotpSecretCipher {
    private static final String PREFIX = "v1:";
    private static final int NONCE_LENGTH = 12;
    private final Path keyPath = Path.of(System.getProperty("user.home"), ".esc-contabil", "totp.key");
    private volatile SecretKey cachedKey;

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] result = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, result, 0, nonce.length);
            System.arraycopy(encrypted, 0, result, nonce.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger o segredo do Authenticator.", exception);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            if (ciphertext == null || !ciphertext.startsWith(PREFIX)) throw new IllegalArgumentException("Formato de segredo TOTP não suportado.");
            byte[] bytes = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
            if (bytes.length <= NONCE_LENGTH) throw new IllegalArgumentException("Segredo TOTP criptografado inválido.");
            byte[] nonce = Arrays.copyOfRange(bytes, 0, NONCE_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(bytes, NONCE_LENGTH, bytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível ler o segredo do Authenticator. Preserve o arquivo de chave do usuário do Windows.", exception);
        }
    }

    private SecretKey key() throws Exception {
        SecretKey current = cachedKey;
        if (current != null) return current;
        synchronized (this) {
            if (cachedKey != null) return cachedKey;
            Files.createDirectories(keyPath.getParent());
            byte[] raw;
            if (Files.exists(keyPath)) {
                raw = Files.readAllBytes(keyPath);
                if (raw.length != 32) throw new IllegalStateException("A chave local do Authenticator tem formato inválido.");
            } else {
                KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(256);
                raw = generator.generateKey().getEncoded();
                try {
                    Files.write(keyPath, raw, java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE);
                } catch (java.nio.file.FileAlreadyExistsException race) {
                    raw = Files.readAllBytes(keyPath);
                }
            }
            cachedKey = new javax.crypto.spec.SecretKeySpec(raw, "AES");
            return cachedKey;
        }
    }
}
