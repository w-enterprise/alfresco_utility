package it.welf.alfresco.tool;

import it.welf.alfresco.tool.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    void testEncryptionDecryption() {
        String original = "MySecretPassword123!";
        String encrypted = CryptoUtil.encrypt(original);
        String decrypted = CryptoUtil.decrypt(encrypted);

        assertNotEquals(original, encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testEmptyString() {
        assertEquals("", CryptoUtil.encrypt(""));
        assertEquals("", CryptoUtil.decrypt(""));
    }
}
