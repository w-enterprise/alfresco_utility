package it.welf.alfresco.tool.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Arrays;

public class CryptoUtil {
    private static final String ALGORITHM = "AES";
    // Using a fixed key for simplicity as per "basic encryption" requirement.
    // In a real production app, this should be handled via KeyStore or OS-level keyring.
    private static final byte[] KEY = Arrays.copyOf("AlfrescoToolKey".getBytes(StandardCharsets.UTF_8), 16);

    public static String encrypt(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            Key key = new SecretKeySpec(KEY, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedByteValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedByteValue);
        } catch (Exception e) {
            e.printStackTrace();
            return value; // Fallback or handle error
        }
    }

    public static String decrypt(String value) {
        if (value == null || value.isEmpty()) return value;
        try {
            Key key = new SecretKeySpec(KEY, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedValue64 = Base64.getDecoder().decode(value);
            byte[] decryptedByteValue = cipher.doFinal(decryptedValue64);
            return new String(decryptedByteValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return empty on error
        }
    }
}
