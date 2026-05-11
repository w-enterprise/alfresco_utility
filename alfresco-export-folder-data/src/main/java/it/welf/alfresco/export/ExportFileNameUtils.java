package it.welf.alfresco.export;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class ExportFileNameUtils {

    private static final Pattern INVALID_WINDOWS_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final int MAX_FILENAME_LEN = 80;

    private ExportFileNameUtils() {
    }

    public static String extractFolderNameFromPath(String path) {
        if (path == null) {
            return "";
        }
        String p = path.trim();
        if (p.isEmpty()) {
            return "";
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        int idx = p.lastIndexOf('/');
        if (idx >= 0 && idx < p.length() - 1) {
            return p.substring(idx + 1);
        }
        return p;
    }

    public static String sanitizeFileNamePart(String s) {
        String v = (s == null) ? "" : s.trim();
        if (v.isEmpty()) {
            v = "export";
        }
        v = INVALID_WINDOWS_CHARS.matcher(v).replaceAll("_");
        v = v.replaceAll("[\\p{Cntrl}]+", "_");
        v = v.replaceAll("\\s+", " ").trim();
        v = v.replaceAll("[._ ]+$", "");
        if (v.isEmpty()) {
            v = "export";
        }
        if (v.length() > MAX_FILENAME_LEN) {
            v = v.substring(0, MAX_FILENAME_LEN).trim();
        }
        return v;
    }

    public static String buildExportFileName(String folderName, LocalDateTime now, String nodeId, int depth) {
        String safeFolder = sanitizeFileNamePart(folderName);
        String tsDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tsTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        String nodeShort = (nodeId == null) ? "" : nodeId.trim();
        if (nodeShort.length() > 8) {
            nodeShort = nodeShort.substring(0, 8);
        }
        String suffix = nodeShort.isEmpty() ? ("d" + depth) : (nodeShort + "_d" + depth);
        return safeFolder + "_" + tsDate + "_" + tsTime + "_" + suffix + ".csv";
    }
}
