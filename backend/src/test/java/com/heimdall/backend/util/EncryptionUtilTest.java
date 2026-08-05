package com.heimdall.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionUtilTest {

    @BeforeEach
    public void setup() {
        EncryptionUtil util = new EncryptionUtil();
        util.setSecretKey("ThisIsATestSecretKey123");
    }

    @Test
    public void testEncryptionAndDecryption() {
        String plainText = "MySuperSecretPassword!";
        String encryptedText = EncryptionUtil.encrypt(plainText);
        
        assertNotNull(encryptedText);
        assertNotEquals(plainText, encryptedText);
        
        String decryptedText = EncryptionUtil.decrypt(encryptedText);
        assertEquals(plainText, decryptedText);
    }
    
    @Test
    public void testNullValues() {
        assertNull(EncryptionUtil.encrypt(null));
        assertNull(EncryptionUtil.decrypt(null));
    }
}
