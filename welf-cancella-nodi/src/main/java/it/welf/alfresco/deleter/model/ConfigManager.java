package it.welf.alfresco.deleter.model;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static final String KEY = "WelfAlfrescoKey!"; // 16 chars for AES-128
    private static final String ALGORITHM = "AES";

    private final Path configPath;

    public ConfigManager() {
        this.configPath = Paths.get(CONFIG_FILE);
    }

    public void saveConfig(String url, String username, String password) throws Exception {
        Properties props = new Properties();
        props.setProperty("url", url);
        props.setProperty("username", username);
        props.setProperty("password", encrypt(password));

        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "Alfresco Deleter Configuration");
        }
    }

    public ConnectionConfig loadConfig() {
        if (!Files.exists(configPath)) {
            return new ConnectionConfig("", "", "");
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            String url = props.getProperty("url", "");
            String username = props.getProperty("username", "");
            String encryptedPwd = props.getProperty("password", "");
            String password = "";
            if (!encryptedPwd.isEmpty()) {
                try {
                    password = decrypt(encryptedPwd);
                } catch (Exception e) {
                    System.err.println("Failed to decrypt password: " + e.getMessage());
                }
            }
            return new ConnectionConfig(url, username, password);
        } catch (IOException e) {
            e.printStackTrace();
            return new ConnectionConfig("", "", "");
        }
    }

    private String encrypt(String value) throws Exception {
        Key key = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decrypt(String encryptedValue) throws Exception {
        Key key = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(encryptedValue);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }

    public static class ConnectionConfig {
        private final String url;
        private final String username;
        private final String password;

        public ConnectionConfig(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }
}
