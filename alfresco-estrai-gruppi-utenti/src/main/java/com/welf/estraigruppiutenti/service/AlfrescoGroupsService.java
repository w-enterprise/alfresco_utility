package com.welf.estraigruppiutenti.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.welf.estraigruppiutenti.model.GroupInfo;
import com.welf.estraigruppiutenti.util.AlfrescoUrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Recupera gruppi e utenti da Alfresco usando le API REST pubbliche (v1).
 */
public class AlfrescoGroupsService {
    private static final Logger logger = LoggerFactory.getLogger(AlfrescoGroupsService.class);

    private final HttpClient httpClient;
    private final String alfrescoBaseUrl;
    private final String authHeaderValue;

    /**
     * Crea il servizio per le API Alfresco.
     *
     * @param alfrescoBaseUrl URL base Alfresco (es. http://host:port/alfresco).
     * @param username        username.
     * @param password        password.
     */
    public AlfrescoGroupsService(String alfrescoBaseUrl, String username, String password) {
        String base = normalizeBase(alfrescoBaseUrl);
        if (!base.toLowerCase().startsWith("http")) {
            base = "http://" + base;
        }
        this.alfrescoBaseUrl = base;
        this.authHeaderValue = "Basic " + Base64.getEncoder().encodeToString((username + ":" + (password == null ? "" : password))
                .getBytes(StandardCharsets.UTF_8));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Recupera tutti i gruppi, ordinati alfabeticamente per nome visualizzato, con conteggio utenti diretti.
     *
     * @param cancellationSupplier supplier che ritorna true se l'operazione è stata cancellata.
     * @return lista gruppi.
     * @throws AlfrescoApiException in caso di errore HTTP/JSON o cancellazione.
     */
    public List<GroupInfo> fetchAllGroups(BooleanSupplier cancellationSupplier) throws AlfrescoApiException {
        List<GroupInfo> groups = new ArrayList<>();

        int skipCount = 0;
        int maxItems = 100;
        Integer totalItems = null;

        while (totalItems == null || skipCount < totalItems) {
            if (cancellationSupplier != null && cancellationSupplier.getAsBoolean()) {
                throw new AlfrescoApiException("Operazione cancellata.");
            }

            String url = alfrescoBaseUrl + "/api/-default-/public/alfresco/versions/1/groups?skipCount=" + skipCount + "&maxItems=" + maxItems;
            JsonObject root = getJson(url, Duration.ofSeconds(30));
            JsonObject list = requireObject(root, "list");
            JsonObject pagination = requireObject(list, "pagination");
            totalItems = pagination.get("totalItems").getAsInt();

            JsonArray entries = requireArray(list, "entries");
            if (entries.size() == 0) {
                break;
            }

            for (JsonElement element : entries) {
                if (cancellationSupplier != null && cancellationSupplier.getAsBoolean()) {
                    throw new AlfrescoApiException("Operazione cancellata.");
                }
                JsonObject entryWrapper = element.getAsJsonObject();
                JsonObject entry = requireObject(entryWrapper, "entry");
                String id = getString(entry, "id");
                String displayName = getString(entry, "displayName");
                String description = getString(entry, "description");
                int directUsers = fetchDirectUsersCount(id, cancellationSupplier);
                groups.add(new GroupInfo(id, displayName, description, directUsers));
            }

            skipCount += entries.size();
        }

        groups.sort(Comparator.comparing(g -> Objects.toString(g.getDisplayName(), ""), String.CASE_INSENSITIVE_ORDER));
        return groups;
    }

    /**
     * Recupera gli username degli utenti membri diretti del gruppo.
     *
     * @param groupId            id del gruppo (es. GROUP_xxx).
     * @param cancellationSupplier supplier che ritorna true se l'operazione è stata cancellata.
     * @return lista username.
     * @throws AlfrescoApiException in caso di errore HTTP/JSON o cancellazione.
     */
    public List<String> fetchGroupUserMembers(String groupId, BooleanSupplier cancellationSupplier) throws AlfrescoApiException {
        List<String> users = new ArrayList<>();

        int skipCount = 0;
        int maxItems = 200;
        Integer totalItems = null;

        String encodedGroupId = AlfrescoUrlUtils.encodePathSegment(groupId);

        while (totalItems == null || skipCount < totalItems) {
            if (cancellationSupplier != null && cancellationSupplier.getAsBoolean()) {
                throw new AlfrescoApiException("Operazione cancellata.");
            }

            String url = alfrescoBaseUrl + "/api/-default-/public/alfresco/versions/1/groups/" + encodedGroupId
                    + "/members?where=(memberType='PERSON')&skipCount=" + skipCount + "&maxItems=" + maxItems;
            JsonObject root = getJson(url, Duration.ofSeconds(30));
            JsonObject list = requireObject(root, "list");
            JsonObject pagination = requireObject(list, "pagination");
            totalItems = pagination.get("totalItems").getAsInt();

            JsonArray entries = requireArray(list, "entries");
            if (entries.size() == 0) {
                break;
            }

            for (JsonElement element : entries) {
                JsonObject entryWrapper = element.getAsJsonObject();
                JsonObject entry = requireObject(entryWrapper, "entry");
                String id = getString(entry, "id");
                if (id != null && !id.isBlank()) {
                    users.add(id);
                }
            }
            skipCount += entries.size();
        }

        return users;
    }

    private int fetchDirectUsersCount(String groupId, BooleanSupplier cancellationSupplier) throws AlfrescoApiException {
        if (cancellationSupplier != null && cancellationSupplier.getAsBoolean()) {
            throw new AlfrescoApiException("Operazione cancellata.");
        }

        String encodedGroupId = AlfrescoUrlUtils.encodePathSegment(groupId);
        String url = alfrescoBaseUrl + "/api/-default-/public/alfresco/versions/1/groups/" + encodedGroupId
                + "/members?where=(memberType='PERSON')&skipCount=0&maxItems=1";
        JsonObject root = getJson(url, Duration.ofSeconds(30));
        JsonObject list = requireObject(root, "list");
        JsonObject pagination = requireObject(list, "pagination");
        return pagination.get("totalItems").getAsInt();
    }

    private JsonObject getJson(String url, Duration timeout) throws AlfrescoApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Authorization", authHeaderValue)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 401) {
                throw new AlfrescoApiException("Autenticazione fallita (401) sulle API Alfresco. Verifica credenziali.");
            }
            if (status == 403) {
                throw new AlfrescoApiException("Permesso negato (403) sulle API Alfresco. Verifica permessi dell'utente.");
            }
            if (status < 200 || status >= 300) {
                throw new AlfrescoApiException("Errore HTTP " + status + " su " + url + ": " + truncate(response.body()));
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (AlfrescoApiException e) {
            throw e;
        } catch (Exception e) {
            logger.debug("Errore durante chiamata API Alfresco: {}", url, e);
            throw new AlfrescoApiException("Errore di rete/timeout durante chiamata API Alfresco: " + safeMessage(e), e);
        }
    }

    private static JsonObject requireObject(JsonObject parent, String name) throws AlfrescoApiException {
        JsonElement el = parent.get(name);
        if (el == null || !el.isJsonObject()) {
            throw new AlfrescoApiException("Risposta JSON non valida: manca oggetto '" + name + "'.");
        }
        return el.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonObject parent, String name) throws AlfrescoApiException {
        JsonElement el = parent.get(name);
        if (el == null || !el.isJsonArray()) {
            throw new AlfrescoApiException("Risposta JSON non valida: manca array '" + name + "'.");
        }
        return el.getAsJsonArray();
    }

    private static String getString(JsonObject obj, String name) {
        JsonElement el = obj.get(name);
        if (el == null || el.isJsonNull()) {
            return "";
        }
        try {
            return el.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return msg == null ? t.getClass().getSimpleName() : msg;
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500) + "...";
    }

    private static String normalizeBase(String base) {
        String url = base == null ? "" : base.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
