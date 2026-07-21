package it.welf.alfresco.folderprops.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.welf.alfresco.folderprops.model.NodeInfo;
import it.welf.alfresco.folderprops.model.ReportItem;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class AlfrescoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoService.class);
    private static final DateTimeFormatter LAST_MODIFIED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> STANDARD_DOCUMENT_TYPES = new HashSet<>(Arrays.asList(
            "cmis:document",
            "cm:content",
            "cm:dictionarymodel",
            "cm:savedquery",
            "d:cm:content",
            "d:cm:dictionarymodel",
            "d:cm:savedquery"
    ));

    private Session session;
    private String baseUrl;
    private String username;
    private String password;
    private boolean rulesEndpointUnsupported;

    public void connect(String url, String username, String password) throws Exception {
        this.baseUrl = extractBaseUrl(url);
        this.username = username;
        this.password = password;
        LOGGER.info("Inizializzo connessione CMIS: url={}, username={}", url, username);

        Map<String, String> parameter = new HashMap<>();
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password);
        parameter.put(SessionParameter.ATOMPUB_URL, url);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        parameter.put(SessionParameter.LOCALE_ISO3166_COUNTRY, "");
        parameter.put(SessionParameter.LOCALE_ISO639_LANGUAGE, "en");

        SessionFactory factory = SessionFactoryImpl.newInstance();
        List<Repository> repositories = factory.getRepositories(parameter);
        if (repositories.isEmpty()) {
            throw new Exception("No repositories found at the specified URL.");
        }
        session = repositories.get(0).createSession();
        session.getDefaultContext().setCacheEnabled(false);
        LOGGER.info("Sessione CMIS creata correttamente (cache disabilitata)");
    }

    public List<NodeInfo> getChildren(String folderId) {
        CmisObject obj = session.getObject(folderId);
        if (obj instanceof Folder) {
            return getChildren((Folder) obj);
        }
        return new ArrayList<>();
    }

    public List<NodeInfo> getChildren(Folder folder) {
        List<NodeInfo> children = new ArrayList<>();
        ItemIterable<CmisObject> objects = folder.getChildren();
        for (CmisObject o : objects) {
            String path = "";
            if (o instanceof FileableCmisObject) {
                List<String> paths = ((FileableCmisObject) o).getPaths();
                if (paths != null && !paths.isEmpty()) {
                    path = paths.get(0);
                }
            }
            children.add(new NodeInfo(o.getId(), o.getName(), o.getType().getId(), path, o instanceof Folder));
        }
        return children;
    }

    public Folder getFolderByPath(String path) {
        if (session == null) {
            throw new IllegalStateException("Sessione CMIS non inizializzata.");
        }
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Percorso nodo vuoto.");
        }

        CmisObject obj = session.getObjectByPath(path.trim());
        if (!(obj instanceof Folder)) {
            throw new IllegalArgumentException("Il nodo indicato non è una cartella: " + path);
        }
        return (Folder) obj;
    }

    public Folder getFolderById(String nodeId) {
        if (session == null) {
            throw new IllegalStateException("Sessione CMIS non inizializzata.");
        }
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("NodeId vuoto.");
        }

        CmisObject obj = session.getObject(nodeId.trim());
        if (!(obj instanceof Folder)) {
            throw new IllegalArgumentException("Il nodo indicato non è una cartella: " + nodeId);
        }
        return (Folder) obj;
    }

    public boolean exportReportRecursive(
            String rootId,
            int maxDepth,
            String txtFile,
            Consumer<String> logger,
            BooleanSupplier cancelRequested
    ) throws IOException {
        if (session != null) {
            session.clear();
        }

        List<ReportItem> foldersWithRules = new ArrayList<>();
        List<ReportItem> filesWithNonStandardClass = new ArrayList<>();

        CmisObject rootObj = session.getObject(rootId);
        ScanResult scanResult = collectReportItemsRecursive(
                rootObj,
                1,
                maxDepth,
                foldersWithRules,
                filesWithNonStandardClass,
                logger,
                cancelRequested
        );

        try {
            TxtReportWriter.writeReport(
                    txtFile,
                    rootObj.getName(),
                    getPath(rootObj),
                    rootObj.getId(),
                    scanResult.totalSizeBytes,
                    foldersWithRules,
                    filesWithNonStandardClass
            );
            return scanResult.cancelled;
        } catch (IOException e) {
            LOGGER.error("Errore scrittura TXT: {}", txtFile, e);
            throw e;
        }
    }

    private ScanResult collectReportItemsRecursive(
            CmisObject obj,
            int currentDepth,
            int maxDepth,
            List<ReportItem> foldersWithRules,
            List<ReportItem> filesWithNonStandardClass,
            Consumer<String> logger,
            BooleanSupplier cancelRequested
    ) {
        if (cancelRequested != null && cancelRequested.getAsBoolean()) {
            logger.accept("[STOP] Interruzione richiesta. Salvataggio risultati parziali...");
            return new ScanResult(true, 0L);
        }

        String path = getPath(obj);

        logger.accept("Processando: " + obj.getName() + " (Profondità: " + currentDepth + ")");

        if (obj instanceof Folder) {
            boolean hasRules = false;
            try {
                int activeRuleCount = getActiveRuleCount(obj.getId());
                hasRules = activeRuleCount > 0;
            } catch (Exception e) {
                logger.accept("Errore recupero regole per " + obj.getName() + ": " + e.getMessage());
                LOGGER.error("Errore recupero regole per {}", obj.getId(), e);
            }

            long folderSizeBytes = 0L;
            if (maxDepth == 0 || currentDepth < maxDepth) {
                ItemIterable<CmisObject> children = ((Folder) obj).getChildren();
                for (CmisObject child : children) {
                    ScanResult childResult = collectReportItemsRecursive(
                            child,
                            currentDepth + 1,
                            maxDepth,
                            foldersWithRules,
                            filesWithNonStandardClass,
                            logger,
                            cancelRequested
                    );
                    folderSizeBytes += childResult.totalSizeBytes;
                    if (childResult.cancelled) {
                        return new ScanResult(true, folderSizeBytes);
                    }
                }

                if (currentDepth % 5 == 0) {
                    session.clear();
                }
            }

            if (hasRules) {
                foldersWithRules.add(new ReportItem(
                        "folder-with-rules",
                        obj.getId(),
                        obj.getName(),
                        path,
                        "",
                        "",
                        "",
                        folderSizeBytes
                ));
            }
            return new ScanResult(false, folderSizeBytes);
        }

        if (obj instanceof Document) {
            long documentSizeBytes = getDocumentSizeBytes((Document) obj);
            String documentType = getDocumentType(obj);
            if (!isStandardDocumentType(documentType)) {
                filesWithNonStandardClass.add(new ReportItem(
                        "file-non-standard-class",
                        obj.getId(),
                        obj.getName(),
                        path,
                        getMimeType(obj),
                        documentType,
                        getLastModified(obj)
                ));
            }
            return new ScanResult(false, documentSizeBytes);
        }

        return new ScanResult(false, 0L);
    }

    public Session getSession() {
        return session;
    }

    private String getPath(CmisObject obj) {
        if (obj instanceof FileableCmisObject) {
            List<String> paths = ((FileableCmisObject) obj).getPaths();
            return (paths != null && !paths.isEmpty()) ? paths.get(0) : "";
        }
        return "";
    }

    private int getActiveRuleCount(String nodeId) throws IOException {
        return folderHasRulesAspect(nodeId) ? 1 : 0;
    }

    private boolean folderHasRulesAspect(String nodeId) {
        if (session == null || nodeId == null || nodeId.trim().isEmpty()) {
            return false;
        }

        try {
            CmisObject obj = session.getObject(nodeId.trim());
            Object secondaryTypes = obj.getPropertyValue("cmis:secondaryObjectTypeIds");
            if (containsRulesMarker(secondaryTypes)) {
                return true;
            }
        } catch (Exception e) {
        }

        try {
            CmisObject obj = session.getObject(nodeId.trim());
            Object aspects = obj.getPropertyValue("alfcmis:nodeAspects");
            if (containsRulesMarker(aspects)) {
                return true;
            }
        } catch (Exception e) {
        }

        return false;
    }

    private boolean containsRulesMarker(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                if (isRulesAspectValue(item)) {
                    return true;
                }
            }
            return false;
        }

        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            for (Object item : array) {
                if (isRulesAspectValue(item)) {
                    return true;
                }
            }
            return false;
        }

        return isRulesAspectValue(value);
    }

    private boolean isRulesAspectValue(Object value) {
        if (value == null) {
            return false;
        }
        String normalized = value.toString().trim().toLowerCase(Locale.ROOT);
        return normalized.equals("p:rule:rules")
                || normalized.equals("rule:rules")
                || normalized.endsWith(":rule:rules");
    }

    private String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8")));

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(is);
            if (code < 200 || code >= 300) {
                LOGGER.warn("REST call fallita: url={}, httpCode={}, body={}", urlStr, code, body);
                throw new HttpStatusException(code, body);
            }
            return body;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String getMimeType(CmisObject obj) {
        if (obj == null) {
            return "";
        }

        try {
            if (obj instanceof Document) {
                String mt = ((Document) obj).getContentStreamMimeType();
                if (mt != null && !mt.trim().isEmpty()) {
                    return mt;
                }
            }
        } catch (Exception e) {
        }

        try {
            Object value = obj.getPropertyValue("cmis:contentStreamMimeType");
            if (value instanceof String) {
                return (String) value;
            }
        } catch (Exception e) {
        }

        return "";
    }

    private String getDocumentType(CmisObject obj) {
        if (obj == null) {
            return "";
        }

        try {
            if (obj.getType() != null && obj.getType().getId() != null) {
                return obj.getType().getId();
            }
        } catch (Exception e) {
        }

        try {
            Object value = obj.getPropertyValue("cmis:objectTypeId");
            if (value instanceof String) {
                return (String) value;
            }
        } catch (Exception e) {
        }

        return "";
    }

    private long getDocumentSizeBytes(Document document) {
        if (document == null) {
            return 0L;
        }

        try {
            long length = document.getContentStreamLength();
            if (length >= 0) {
                return length;
            }
        } catch (Exception e) {
        }

        try {
            Object value = document.getPropertyValue("cmis:contentStreamLength");
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (Exception e) {
        }

        return 0L;
    }

    private String getLastModified(CmisObject obj) {
        if (obj == null) {
            return "";
        }

        try {
            if (obj instanceof Document) {
                GregorianCalendar calendar = ((Document) obj).getLastModificationDate();
                if (calendar != null) {
                    return LAST_MODIFIED_FORMATTER.format(calendar.toZonedDateTime());
                }
            }
        } catch (Exception e) {
        }

        try {
            Object value = obj.getPropertyValue("cmis:lastModificationDate");
            if (value instanceof GregorianCalendar) {
                GregorianCalendar calendar = (GregorianCalendar) value;
                return LAST_MODIFIED_FORMATTER.format(calendar.toZonedDateTime().withZoneSameInstant(ZoneId.systemDefault()));
            }
        } catch (Exception e) {
        }

        return "";
    }

    static boolean isStandardDocumentType(String documentType) {
        String normalized = normalizeDocumentType(documentType);
        if (normalized.isEmpty()) {
            return false;
        }
        return STANDARD_DOCUMENT_TYPES.contains(normalized);
    }

    static int countActiveRulesFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return 0;
        }

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject list = root.getAsJsonObject("list");
        if (list == null) {
            return 0;
        }

        JsonArray entries = list.getAsJsonArray("entries");
        if (entries == null) {
            return 0;
        }

        int count = 0;
        for (JsonElement el : entries) {
            if (el == null || !el.isJsonObject()) {
                continue;
            }

            JsonObject wrapper = el.getAsJsonObject();
            JsonObject entry = wrapper.has("entry") && wrapper.get("entry").isJsonObject()
                    ? wrapper.getAsJsonObject("entry")
                    : wrapper;

            Boolean enabled = null;
            if (entry.has("isEnabled")) {
                enabled = entry.get("isEnabled").getAsBoolean();
            } else if (entry.has("enabled")) {
                enabled = entry.get("enabled").getAsBoolean();
            } else if (entry.has("disabled")) {
                enabled = !entry.get("disabled").getAsBoolean();
            } else if (entry.has("isDisabled")) {
                enabled = !entry.get("isDisabled").getAsBoolean();
            }

            if (enabled == null || enabled) {
                count++;
            }
        }

        return count;
    }

    private static String normalizeDocumentType(String documentType) {
        if (documentType == null) {
            return "";
        }
        return documentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extractBaseUrl(String cmisUrl) {
        if (cmisUrl == null) {
            return null;
        }
        int idx = cmisUrl.indexOf("/alfresco/");
        if (idx >= 0) {
            return cmisUrl.substring(0, idx + "/alfresco".length());
        }
        idx = cmisUrl.indexOf("/alfresco");
        if (idx >= 0) {
            return cmisUrl.substring(0, idx + "/alfresco".length());
        }
        return cmisUrl.replaceAll("/+$", "");
    }

    private static class HttpStatusException extends IOException {
        private final int statusCode;

        HttpStatusException(int statusCode, String body) {
            super("HTTP " + statusCode + " - " + body);
            this.statusCode = statusCode;
        }

        int getStatusCode() {
            return statusCode;
        }
    }

    private static class ScanResult {
        private final boolean cancelled;
        private final long totalSizeBytes;

        private ScanResult(boolean cancelled, long totalSizeBytes) {
            this.cancelled = cancelled;
            this.totalSizeBytes = totalSizeBytes;
        }
    }
}
