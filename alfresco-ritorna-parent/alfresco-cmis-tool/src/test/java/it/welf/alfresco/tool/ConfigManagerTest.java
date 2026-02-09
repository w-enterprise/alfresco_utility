package it.welf.alfresco.tool;

import it.welf.alfresco.tool.util.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @Test
    void testSaveAndLoad(@TempDir Path tempDir) {
        File tempFile = tempDir.resolve("test_config.properties").toFile();
        ConfigManager config = new ConfigManager(tempFile.getAbsolutePath());

        config.save("http://localhost:8080/alfresco", "admin", "admin");

        // Create a new instance to simulate restart
        ConfigManager loadedConfig = new ConfigManager(tempFile.getAbsolutePath());
        loadedConfig.load();

        assertEquals("http://localhost:8080/alfresco", loadedConfig.getUrl());
        assertEquals("admin", loadedConfig.getUsername());
        assertEquals("admin", loadedConfig.getPassword());
    }
}
