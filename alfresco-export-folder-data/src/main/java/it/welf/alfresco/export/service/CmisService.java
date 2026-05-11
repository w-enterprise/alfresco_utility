package it.welf.alfresco.export.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.welf.alfresco.export.model.NodeInfo;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class CmisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmisService.class);

    private Session session;
    private String baseUrl;
    private String username;
    private String password;

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
        LOGGER.info("Sessione CMIS creata correttamente");
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

    public void exportPermissionsRecursive(String rootId, int maxDepth, String csvFile, Consumer<String> logger) throws IOException {
        exportPermissionsRecursive(rootId, maxDepth, csvFile, logger, null);
    }

    public boolean exportPermissionsRecursive(
            String rootId,
            int maxDepth,
            String csvFile,
            Consumer<String> logger,
            BooleanSupplier cancelRequested
    ) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("nodeId;path;principalId;origine;ruolo;numeroFile;numeroCartelle");
            CmisObject rootObj = session.getObject(rootId);
            boolean cancelled = processNode(rootObj, 1, maxDepth, writer, logger, cancelRequested);
            writer.flush();
            return cancelled;
        } catch (IOException e) {
            LOGGER.error("Errore scrittura CSV: {}", csvFile, e);
            throw e;
        }
    }

    private boolean processNode(
            CmisObject obj,
            int currentDepth,
            int maxDepth,
            PrintWriter writer,
            Consumer<String> logger,
            BooleanSupplier cancelRequested
    ) {
        if (cancelRequested != null && cancelRequested.getAsBoolean()) {
            logger.accept("[STOP] Interruzione richiesta. Salvataggio risultati parziali...");
            writer.flush();
            return true;
        }

        String path = "";
        if (obj instanceof FileableCmisObject) {
            List<String> paths = ((FileableCmisObject) obj).getPaths();
            path = (paths != null && !paths.isEmpty()) ? paths.get(0) : "";
        }

        logger.accept("Processando: " + obj.getName() + " (Profondità: " + currentDepth + ")");

        List<CmisObject> childrenList = null;
        int fileCount = 0;
        int folderCount = 0;

        if (obj instanceof Folder) {
            childrenList = new ArrayList<>();
            try {
                ItemIterable<CmisObject> children = ((Folder) obj).getChildren();
                for (CmisObject child : children) {
                    if (cancelRequested != null && cancelRequested.getAsBoolean()) {
                        logger.accept("[STOP] Interruzione richiesta. Salvataggio risultati parziali...");
                        writer.flush();
                        return true;
                    }
                    childrenList.add(child);
                    if (child instanceof Folder) {
                        folderCount++;
                    } else {
                        fileCount++;
                    }
                }
            } catch (Exception e) {
                String msg = "Errore conteggio contenuti per " + obj.getName() + ": " + e.getMessage();
                logger.accept(msg);
                LOGGER.error(msg, e);
            }
        }

        // Estrai permessi SOLO se è una cartella (cmis:folder)
        if (obj instanceof Folder) {
            try {
                List<RoleEntry> roles = getNodeRoleEntries(obj.getId(), logger, cancelRequested);
                for (RoleEntry role : roles) {
                    if (cancelRequested != null && cancelRequested.getAsBoolean()) {
                        logger.accept("[STOP] Interruzione richiesta. Salvataggio risultati parziali...");
                        writer.flush();
                        return true;
                    }
                    writer.println(obj.getId() + ";" + path + ";" + role.principalId + ";" + role.origine + ";" + role.ruolo + ";" + fileCount + ";" + folderCount);
                }
                writer.flush();
            } catch (Exception e) {
                String msg = "Errore ruoli per " + obj.getName() + ": " + e.getMessage();
                logger.accept(msg);
                LOGGER.error(msg, e);
            }
        }

        // Ricorsione se cartella e profondità lo permette
        if (obj instanceof Folder && (maxDepth == 0 || currentDepth < maxDepth)) {
            if (childrenList == null) {
                childrenList = new ArrayList<>();
                ItemIterable<CmisObject> children = ((Folder) obj).getChildren();
                for (CmisObject child : children) {
                    childrenList.add(child);
                }
            }

            for (CmisObject child : childrenList) {
                boolean cancelled = processNode(child, currentDepth + 1, maxDepth, writer, logger, cancelRequested);
                if (cancelled) {
                    return true;
                }
            }
        }

        return false;
    }

    public Session getSession() {
        return session;
    }

    private static class RoleEntry {
        private final String principalId;
        private final String origine;
        private final String ruolo;

        private RoleEntry(String principalId, String origine, String ruolo) {
            this.principalId = principalId;
            this.origine = origine;
            this.ruolo = ruolo;
        }
    }

    private List<RoleEntry> getNodeRoleEntries(String nodeId, Consumer<String> logger, BooleanSupplier cancelRequested) throws IOException {
        List<RoleEntry> result = new ArrayList<>();
        if (baseUrl == null || username == null || password == null) {
            return result;
        }

        if (cancelRequested != null && cancelRequested.getAsBoolean()) {
            return result;
        }

        String encodedNodeId = URLEncoder.encode(nodeId, "UTF-8");
        String url = baseUrl + "/api/-default-/public/alfresco/versions/1/nodes/" + encodedNodeId + "?include=permissions";
        String json = httpGet(url);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject entry = root.getAsJsonObject("entry");
        if (entry == null) {
            return result;
        }

        JsonObject permissions = entry.getAsJsonObject("permissions");
        if (permissions == null) {
            return result;
        }

        JsonArray locallySet = permissions.getAsJsonArray("locallySet");
        JsonArray inherited = permissions.getAsJsonArray("inherited");

        addRoleEntriesFromArray(result, locallySet, "settato");
        addRoleEntriesFromArray(result, inherited, "ereditato");

        if (result.isEmpty()) {
            logger.accept("Nessun ruolo trovato per nodeId=" + nodeId);
            LOGGER.debug("Nessun ruolo trovato per nodeId={}", nodeId);
        }

        return result;
    }

    private void addRoleEntriesFromArray(List<RoleEntry> out, JsonArray arr, String origine) {
        if (arr == null) {
            return;
        }
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String accessStatus = getAsString(o, "accessStatus");
            if (accessStatus != null && !"ALLOWED".equalsIgnoreCase(accessStatus)) {
                continue;
            }
            String authorityId = getAsString(o, "authorityId");
            String name = getAsString(o, "name");
            if (authorityId == null || authorityId.isEmpty() || name == null || name.isEmpty()) {
                continue;
            }
            out.add(new RoleEntry(authorityId, origine, name));
        }
    }

    private String getAsString(JsonObject o, String name) {
        JsonElement el = o.get(name);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        return el.getAsString();
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
                throw new IOException("HTTP " + code + " - " + body);
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
}
