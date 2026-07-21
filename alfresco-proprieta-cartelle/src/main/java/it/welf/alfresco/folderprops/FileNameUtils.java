package it.welf.alfresco.folderprops;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileNameUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static String buildReportFileName(String folderName, LocalDateTime dateTime, String nodeId, int depth) {
        String safeName = sanitizeFileNamePart(folderName);
        String safeId = sanitizeFileNamePart(nodeId);
        String depthStr = (depth == 0) ? "tutti" : String.valueOf(depth);
        return safeName + "_" + dateTime.format(DATE_FORMATTER) + "_" + safeId + "_" + depthStr + ".txt";
    }

    public static String sanitizeFileNamePart(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
