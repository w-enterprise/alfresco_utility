package it.welf.alfresco.tool.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private final String configFile;
    private static final String DEFAULT_CONFIG_FILE = "alfresco_tool_config.properties";
    private static final String KEY_URL = "url";
    private static final String KEY_USER = "user";
    private static final String KEY_PASS = "password";

    private String url;
    private String username;
    private String password;

    public ConfigManager() {
        this(DEFAULT_CONFIG_FILE);
    }

    public ConfigManager(String configFile) {
        this.configFile = configFile;
    }

    public void load() {
        File file = new File(configFile);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            this.url = props.getProperty(KEY_URL, "");
            this.username = props.getProperty(KEY_USER, "");
            String encryptedPass = props.getProperty(KEY_PASS, "");
            this.password = CryptoUtil.decrypt(encryptedPass);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;

        Properties props = new Properties();
        props.setProperty(KEY_URL, url);
        props.setProperty(KEY_USER, username);
        props.setProperty(KEY_PASS, CryptoUtil.encrypt(password));

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Alfresco CMIS Tool Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
