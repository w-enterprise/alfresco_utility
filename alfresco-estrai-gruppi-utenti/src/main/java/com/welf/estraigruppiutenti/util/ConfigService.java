package com.welf.estraigruppiutenti.util;

import com.welf.estraigruppiutenti.model.ConnectionConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;
import java.util.Properties;

/**
 * Gestisce il salvataggio e il caricamento automatico della configurazione locale.
 */
public class ConfigService {
    private static final String CONFIG_FILE = "config.properties";
    private static final String KEY_VALUE = "WelfAlfrescoKey!";
    private static final String ALGORITHM = "AES";

    private final Path configPath;

    /**
     * Crea un'istanza che opera sul file config.properties nella working directory.
     */
    public ConfigService() {
        this.configPath = Paths.get(CONFIG_FILE);
    }

    /**
     * Carica la configurazione dal file locale.
     *
     * @return configurazione caricata, oppure valori vuoti se il file non esiste o non è leggibile.
     */
    public ConnectionConfig load() {
        if (!Files.exists(configPath)) {
            return new ConnectionConfig("", "", "");
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
            String url = properties.getProperty("url", "");
            String username = properties.getProperty("username", "");
            String encryptedPwd = properties.getProperty("password", "");
            String password = "";
            if (!encryptedPwd.isBlank()) {
                try {
                    password = decrypt(encryptedPwd);
                } catch (Exception ignored) {
                    password = "";
                }
            }
            return new ConnectionConfig(url, username, password);
        } catch (Exception e) {
            return new ConnectionConfig("", "", "");
        }
    }

    /**
     * Salva la configurazione nel file locale.
     *
     * @param config configurazione da salvare.
     * @throws Exception se non è possibile scrivere o cifrare la password.
     */
    public void save(ConnectionConfig config) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("url", nullToEmpty(config.getUrl()));
        properties.setProperty("username", nullToEmpty(config.getUsername()));
        properties.setProperty("password", encrypt(nullToEmpty(config.getPassword())));

        try (OutputStream out = Files.newOutputStream(configPath)) {
            properties.store(out, "Alfresco Gruppi/Utenti Extractor Configuration");
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String encrypt(String value) throws Exception {
        Key key = new SecretKeySpec(KEY_VALUE.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String decrypt(String encryptedValue) throws Exception {
        Key key = new SecretKeySpec(KEY_VALUE.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(encryptedValue);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
