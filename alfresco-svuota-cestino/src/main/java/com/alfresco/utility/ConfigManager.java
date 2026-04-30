package com.alfresco.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ConfigManager {

    private static final String CONFIG_FILE = "alfresco_utility_config.json";
    private static final String KEY = "AlfrescoUtilityK"; // 16 chars for AES-128
    private final Gson gson;

    public ConfigManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public static class Config {
        public String serverUrl;
        public String encryptedUsername;
    }

    public void saveConfig(String serverUrl, String username) {
        try {
            Config config = new Config();
            config.serverUrl = serverUrl;
            config.encryptedUsername = encrypt(username);
            
            try (Writer writer = new FileWriter(CONFIG_FILE)) {
                gson.toJson(config, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Config loadConfig() {
        if (!Files.exists(Paths.get(CONFIG_FILE))) {
            return null;
        }
        try (Reader reader = new FileReader(CONFIG_FILE)) {
            Config config = gson.fromJson(reader, Config.class);
            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public String decryptUsername(String encrypted) {
        try {
            return decrypt(encrypted);
        } catch (Exception e) {
            return "";
        }
    }

    public void clearConfig() {
        try {
            Files.deleteIfExists(Paths.get(CONFIG_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Simple AES Encryption
    private String encrypt(String value) throws Exception {
        if (value == null) return null;
        SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decrypt(String encrypted) throws Exception {
        if (encrypted == null) return null;
        SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        return new String(original);
    }
}
