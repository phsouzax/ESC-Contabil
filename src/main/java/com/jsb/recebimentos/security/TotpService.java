package com.jsb.recebimentos.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Service
public class TotpService {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String ISSUER = "ESC Contábil";
    private static final int STEP_SECONDS = 30;

    public String newSecret() {
        byte[] secret = new byte[20];
        new SecureRandom().nextBytes(secret);
        return encodeBase32(secret);
    }

    public boolean verify(String secret, String suppliedCode) {
        if (secret == null || suppliedCode == null || !suppliedCode.matches("\\d{6}")) return false;
        long currentStep = System.currentTimeMillis() / 1000 / STEP_SECONDS;
        for (long drift = -1; drift <= 1; drift++) {
            String expected = code(secret, currentStep + drift);
            if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                    suppliedCode.getBytes(StandardCharsets.US_ASCII))) return true;
        }
        return false;
    }

    public String qrDataUri(String secret, String username) {
        try {
            String label = urlEncode(ISSUER) + ":" + urlEncode(username);
            String uri = "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + urlEncode(ISSUER)
                    + "&algorithm=SHA1&digits=6&period=" + STEP_SECONDS;
            BitMatrix matrix = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 240, 240);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) image.setRGB(x, y, matrix.get(x, y) ? 0xff000000 : 0xffffffff);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o QR Code do Authenticator.", exception);
        }
    }

    String code(String secret, long timeStep) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] counter = ByteBuffer.allocate(Long.BYTES).putLong(timeStep).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Segredo TOTP inválido.", exception);
        }
    }

    private byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").toUpperCase();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int buffer = 0, bitsLeft = 0;
        for (char c : normalized.toCharArray()) {
            int index = BASE32.indexOf(c);
            if (index < 0) throw new IllegalArgumentException("Caractere inválido no segredo TOTP.");
            buffer = (buffer << 5) | index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return output.toByteArray();
    }

    private String encodeBase32(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32.charAt((buffer >> (bitsLeft - 5)) & 0x1f));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) result.append(BASE32.charAt((buffer << (5 - bitsLeft)) & 0x1f));
        return result.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
