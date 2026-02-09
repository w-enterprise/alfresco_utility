package it.welf.alfresco.deleter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvServiceTest {

    @Test
    void testReadNodeIds(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("test.csv").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Node ID,Name\n");
            writer.write("123-abc,Node1\n");
            writer.write("456-def,Node2\n");
        }

        CsvService service = new CsvService();
        List<String> ids = service.readNodeIds(file);

        assertEquals(2, ids.size());
        assertEquals("123-abc", ids.get(0));
        assertEquals("456-def", ids.get(1));
    }
}
