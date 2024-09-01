package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String CHARSET = "UTF-8";

    private final SecretKey secretKey;


    public EncryptionService(@Value("${encryption.secret.key}") String secretKeyBase64) {

        // Decode the base64 encoded string to get the secret key bytes
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyBase64);

        // Create a SecretKey object from the decoded bytes
        this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }


    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(CHARSET));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, CHARSET);
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting", e);
        }
    }
}