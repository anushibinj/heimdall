package com.heimdall.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static String secretKey;

    @Value("${heimdall.encryption.secret-key}")
    public void setSecretKey(String key) {
        EncryptionUtil.secretKey = key;
    }

    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // Use only the first 16 bytes for a 128-bit key if the string is longer
            byte[] key = new byte[16];
            byte[] originalKey = secretKey.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(originalKey, 0, key, 0, Math.min(originalKey.length, 16));
            
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public static String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            byte[] key = new byte[16];
            byte[] originalKey = secretKey.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(originalKey, 0, key, 0, Math.min(originalKey.length, 16));
            
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | javax.crypto.IllegalBlockSizeException | javax.crypto.BadPaddingException e) {
            // Fallback for legacy plaintext passwords in the database
            return encryptedText;
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
}
