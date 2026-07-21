package it.welf.alfresco.folderprops.service;

import it.welf.alfresco.folderprops.model.ReportItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TxtReportWriter {
    public static String formatReport(String rootName, String rootPath, String rootNodeId, long totalSizeBytes,
                                      List<ReportItem> foldersWithRules, List<ReportItem> nonStandardFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORT ANALISI ALFRESCO\n");
        sb.append("======================\n");
        sb.append("Cartella analizzata: ").append(safe(rootName)).append("\n");
        sb.append("Percorso: ").append(safe(rootPath)).append("\n");
        sb.append("Node ID: ").append(normalizeNodeId(rootNodeId)).append("\n");
        sb.append("Peso complessivo analizzato: ").append(formatSize(totalSizeBytes)).append("\n\n");
        sb.append("SEZIONE 1 - CARTELLE CON REGOLE CONFIGURATE\n");
        sb.append("===========================================\n");
        sb.append(formatFolderTable(foldersWithRules));
        sb.append("\n\n");


        sb.append("SEZIONE 2 - FILE NON ASSOCIATI ALLA CLASSE DOCUMENTALE STANDARD\n");
        sb.append("===============================================================\n");
        sb.append(formatFileTable(nonStandardFiles));
        sb.append("\n");

        return sb.toString();
    }

    public static void writeReport(String txtFilePath, String rootName, String rootPath, String rootNodeId, long totalSizeBytes,
                                   List<ReportItem> foldersWithRules, List<ReportItem> nonStandardFiles) throws IOException {
        File targetFile = new File(txtFilePath);
        File parent = targetFile.getAbsoluteFile().getParentFile();
        if (parent != null) {
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Impossibile creare la directory di output: " + parent.getAbsolutePath());
            }
            if (!parent.canWrite()) {
                throw new IOException("Directory di output non scrivibile: " + parent.getAbsolutePath());
            }
        }

        if (targetFile.exists() && !targetFile.canWrite()) {
            throw new IOException("File di output non scrivibile: " + targetFile.getAbsolutePath());
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
            writer.write(formatReport(rootName, rootPath, rootNodeId, totalSizeBytes, foldersWithRules, nonStandardFiles));
            writer.flush();
        }

        if (!targetFile.exists() || !targetFile.isFile() || targetFile.length() < 0) {
            throw new IOException("Salvataggio del file TXT non riuscito: " + targetFile.getAbsolutePath());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String formatFolderTable(List<ReportItem> folders) {
        String[] headers = new String[]{"Nome", "Percorso", "Node ID"};
        String[][] rows = new String[folders == null ? 0 : folders.size()][3];

        if (folders != null) {
            for (int i = 0; i < folders.size(); i++) {
                ReportItem item = folders.get(i);
                rows[i][0] = safe(item.getName());
                rows[i][1] = safe(item.getPath());
                rows[i][2] = normalizeNodeId(item.getNodeId());
            }
        }

        return formatTable(headers, rows);
    }

    private static String formatFileTable(List<ReportItem> files) {
        String[] headers = new String[]{"Nome", "Percorso", "Node ID", "MIME Type", "Classe documentale", "Ultima modifica"};
        String[][] rows = new String[files == null ? 0 : files.size()][6];

        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                ReportItem item = files.get(i);
                rows[i][0] = safe(item.getName());
                rows[i][1] = safe(item.getPath());
                rows[i][2] = normalizeNodeId(item.getNodeId());
                rows[i][3] = safe(item.getMimeType());
                rows[i][4] = safe(item.getDocumentType());
                rows[i][5] = safe(item.getLastModified());
            }
        }

        return formatTable(headers, rows);
    }

    private static String formatTable(String[] headers, String[][] rows) {
        int colCount = headers.length;
        int[] widths = new int[colCount];
        for (int c = 0; c < colCount; c++) {
            widths[c] = headers[c].length();
        }
        if (rows != null) {
            for (String[] row : rows) {
                if (row == null) continue;
                for (int c = 0; c < colCount && c < row.length; c++) {
                    String v = row[c] == null ? "" : row[c];
                    if (v.length() > widths[c]) widths[c] = v.length();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(formatRow(headers, widths)).append("\n");
        if (rows != null) {
            for (String[] row : rows) {
                sb.append(formatRow(row, widths)).append("\n");
            }
        }
        return sb.toString();
    }

    private static String formatRow(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            if (c > 0) sb.append("|");
            String v = (values != null && c < values.length && values[c] != null) ? values[c] : "";
            sb.append(padRight(v, widths[c]));
        }
        return sb.toString();
    }

    private static String padRight(String value, int width) {
        String v = value == null ? "" : value;
        if (v.length() >= width) return v;
        StringBuilder sb = new StringBuilder(v);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static String normalizeNodeId(String nodeId) {
        String id = safe(nodeId).trim();
        int semicolonIdx = id.indexOf(';');
        if (semicolonIdx >= 0) {
            id = id.substring(0, semicolonIdx);
        }
        int slashIdx = id.lastIndexOf('/');
        if (slashIdx >= 0 && slashIdx < id.length() - 1) {
            id = id.substring(slashIdx + 1);
        }
        return id;
    }

    private static String formatSize(long bytes) {
        if (bytes < 0) {
            return "";
        }

        double kb = bytes / 1024.0;
        return String.format(java.util.Locale.ROOT, "%.2f KB (%d B)", kb, bytes);
    }
}
