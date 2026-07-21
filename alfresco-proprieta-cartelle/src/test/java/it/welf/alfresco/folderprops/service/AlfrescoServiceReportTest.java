package it.welf.alfresco.folderprops.service;

import it.welf.alfresco.folderprops.FileNameUtils;
import it.welf.alfresco.folderprops.model.ReportItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlfrescoServiceReportTest {

    @Test
    void countActiveRulesFromJson_handlesEnabledDisabledVariants() {
        String json = "{\n" +
                "  \"list\": {\n" +
                "    \"entries\": [\n" +
                "      {\"entry\": {\"isEnabled\": true}},\n" +
                "      {\"entry\": {\"isEnabled\": false}},\n" +
                "      {\"entry\": {\"disabled\": true}},\n" +
                "      {\"entry\": {\"disabled\": false}},\n" +
                "      {\"entry\": {\"name\": \"default-enabled\"}}\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        assertEquals(3, AlfrescoService.countActiveRulesFromJson(json));
    }

    @Test
    void standardDocumentTypeFilter_excludesOnlyStandardClasses() {
        assertTrue(AlfrescoService.isStandardDocumentType("cmis:document"));
        assertTrue(AlfrescoService.isStandardDocumentType("cm:content"));
        assertFalse(AlfrescoService.isStandardDocumentType("custom:contratto"));
        assertFalse(AlfrescoService.isStandardDocumentType(null));
    }

    @Test
    void txtReport_containsTwoReadableSectionsAndNoCsvHeader() {
        String report = TxtReportWriter.formatReport(
                "Area Test",
                "/Sites/test/documentLibrary/Area Test",
                "workspace://SpacesStore/root01;1.0",
                8192L,
                Collections.singletonList(
                        new ReportItem("folder-with-rules", "workspace://SpacesStore/f01;1.0", "Cartella Regole", "/Sites/test/documentLibrary/Cartella Regole", "", "", "", 4096L)
                ),
                Collections.singletonList(
                        new ReportItem("file-non-standard-class", "workspace://SpacesStore/d01;1.0", "Contratto.pdf", "/Sites/test/documentLibrary/Cartella Regole/Contratto.pdf", "application/pdf", "custom:contratto", "2026-07-16 10:00:00")
                )
        );

        assertTrue(report.contains("REPORT ANALISI ALFRESCO"));
        assertTrue(report.contains("Peso complessivo analizzato: 8.00 KB (8192 B)"));
        assertTrue(report.contains("Node ID: root01"));
        assertTrue(report.contains("SEZIONE 1 - CARTELLE CON REGOLE CONFIGURATE"));
        assertTrue(report.contains("SEZIONE 2 - FILE NON ASSOCIATI ALLA CLASSE DOCUMENTALE STANDARD"));
        assertTrue(report.contains("Nome") && report.contains("Percorso") && report.contains("Node ID"));
        assertTrue(report.contains("Cartella Regole"));
        assertTrue(report.contains("Contratto.pdf"));
        assertTrue(report.contains("MIME Type") && report.contains("Classe documentale") && report.contains("Ultima modifica"));
        assertTrue(report.contains("custom:contratto"));
        assertTrue(report.contains("f01"));
        assertTrue(report.contains("d01"));
        assertFalse(report.contains("workspace://"));
        assertFalse(report.contains(";1.0"));
        assertFalse(report.contains("path;nodeId;nome;mimeType"));
    }

    @Test
    void txtReport_handlesEmptyCategoriesWithoutIncludingForeignItems() {
        String report = TxtReportWriter.formatReport("Root", "/Root", "id-root", 0L, Collections.<ReportItem>emptyList(), Arrays.<ReportItem>asList());
        assertTrue(report.contains("Nome") && report.contains("Percorso") && report.contains("Node ID"));
        assertTrue(report.contains("MIME Type") && report.contains("Classe documentale") && report.contains("Ultima modifica"));
        assertTrue(report.contains("Peso complessivo analizzato: 0.00 KB (0 B)"));
        assertFalse(report.contains("mimeType;"));
    }

    @Test
    void generatedFileName_usesTxtExtensionOnly() {
        String fileName = FileNameUtils.buildReportFileName("Cartella", LocalDateTime.of(2026, 7, 16, 10, 30, 0), "abc123", 0);
        assertTrue(fileName.endsWith(".txt"));
        assertFalse(fileName.endsWith(".csv"));
    }

    @Test
    void rulesAspectMarkers_areRecognizedWithoutRestEndpoint() {
        assertTrue(invokeIsRulesAspectValue("P:rule:rules"));
        assertTrue(invokeIsRulesAspectValue("rule:rules"));
        assertTrue(invokeContainsRulesMarker(Arrays.asList("cm:titled", "P:rule:rules")));
        assertFalse(invokeContainsRulesMarker(Arrays.asList("cm:titled", "cm:auditable")));
    }

    @Test
    void report_stripsVersionAndPrefixFromNodeId() {
        String report = TxtReportWriter.formatReport(
                "Root",
                "/Root",
                "workspace://SpacesStore/3427c3d2-0421-4aba-89da-e9421b6d3cb0;1.0",
                0L,
                Collections.singletonList(
                        new ReportItem("folder-with-rules", "workspace://SpacesStore/3427c3d2-0421-4aba-89da-e9421b6d3cb0;1.0", "cosa facciamo oggi", "/Siti/testwe/documentLibrary/cosa facciamo oggi", "", "", "")
                ),
                Collections.emptyList()
        );

        assertTrue(report.contains("3427c3d2-0421-4aba-89da-e9421b6d3cb0"));
        assertFalse(report.contains("workspace://SpacesStore/3427c3d2-0421-4aba-89da-e9421b6d3cb0"));
        assertFalse(report.contains("3427c3d2-0421-4aba-89da-e9421b6d3cb0;1.0"));
    }

    private boolean invokeIsRulesAspectValue(String value) {
        try {
            java.lang.reflect.Method method = AlfrescoService.class.getDeclaredMethod("isRulesAspectValue", Object.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(new AlfrescoService(), value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean invokeContainsRulesMarker(Object value) {
        try {
            java.lang.reflect.Method method = AlfrescoService.class.getDeclaredMethod("containsRulesMarker", Object.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(new AlfrescoService(), value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
