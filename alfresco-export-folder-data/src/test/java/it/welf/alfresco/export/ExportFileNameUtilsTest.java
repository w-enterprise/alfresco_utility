package it.welf.alfresco.export;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ExportFileNameUtilsTest {

    @Test
    void extractFolderNameFromPath_shouldHandleTrailingSlash() {
        assertEquals("documentLibrary", ExportFileNameUtils.extractFolderNameFromPath("/Sites/test/documentLibrary/"));
    }

    @Test
    void extractFolderNameFromPath_shouldHandleUnicode() {
        assertEquals("CONTENZIOSO PENALE", ExportFileNameUtils.extractFolderNameFromPath("/Sites/a/documentLibrary/CONTEZIOSI/CONTENZIOSO PENALE"));
    }

    @Test
    void extractFolderNameFromPath_blankShouldReturnEmpty() {
        assertEquals("", ExportFileNameUtils.extractFolderNameFromPath("   "));
        assertEquals("", ExportFileNameUtils.extractFolderNameFromPath(null));
    }

    @Test
    void sanitizeFileNamePart_shouldReplaceInvalidWindowsChars() {
        assertEquals("A_B_C_D_E_F_G_H_I", ExportFileNameUtils.sanitizeFileNamePart("A\\B/C:D*E?F\"G<H>I|"));
    }

    @Test
    void buildExportFileName_shouldFollowPatternAndIncludeIdentifiers() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 6, 14, 25, 14);
        String file = ExportFileNameUtils.buildExportFileName("Cartella Test", now, "2da19566-d80c-4b78-9623-92b27c60df25", 0);
        assertTrue(file.startsWith("Cartella Test_20260506_142514_2da19566_d0.csv"));
        assertTrue(file.endsWith(".csv"));
    }
}

