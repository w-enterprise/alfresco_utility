package com.alfresco.utility;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class CmisService {

    private Session session;
    private String serverUrl;
    private String username;
    private String password;

    public void connect(String serverUrl, String username, String password) throws Exception {
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;

        // Normalize URL for CMIS
        String cmisUrl = serverUrl;
        if (!cmisUrl.endsWith("/atom") && !cmisUrl.endsWith("/browser")) {
            // Try to guess or append standard CMIS 1.1 AtomPub path if just base URL is given
            if (cmisUrl.endsWith("/alfresco")) {
                cmisUrl += "/api/-default-/public/cmis/versions/1.1/atom";
            } else if (cmisUrl.endsWith("/")) {
                cmisUrl += "api/-default-/public/cmis/versions/1.1/atom";
            } else {
                 cmisUrl += "/alfresco/api/-default-/public/cmis/versions/1.1/atom";
            }
        }

        Map<String, String> parameter = new HashMap<>();
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password);
        parameter.put(SessionParameter.ATOMPUB_URL, cmisUrl);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        parameter.put(SessionParameter.OBJECT_FACTORY_CLASS, "org.apache.chemistry.opencmis.client.impl.repository.ObjectFactoryImpl");

        SessionFactory factory = SessionFactoryImpl.newInstance();
        List<Repository> repositories = factory.getRepositories(parameter);
        
        if (repositories != null && !repositories.isEmpty()) {
            this.session = repositories.get(0).createSession();
        } else {
            throw new Exception("Nessun repository trovato.");
        }
    }

    public boolean isConnected() {
        return session != null;
    }

    private CloseableHttpClient createHttpClient() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        return HttpClients.custom().setDefaultCredentialsProvider(provider).build();
    }

    public long calculateTrashcanSize(ProgressCallback callback) throws Exception {
        // Alfresco API v1 trashcan
        String apiUrl = normalizeBaseUrl(serverUrl) + "/api/-default-/public/alfresco/versions/1/trashcan?include=properties";
        long totalSize = 0;
        boolean hasMore = true;
        int skipCount = 0;
        int maxItems = 100;

        try (CloseableHttpClient client = createHttpClient()) {
            while (hasMore) {
                if (callback != null && callback.isCancelled()) break;
                
                String pagedUrl = apiUrl + "&skipCount=" + skipCount + "&maxItems=" + maxItems;
                HttpGet get = new HttpGet(pagedUrl);
                
                try (CloseableHttpResponse response = client.execute(get)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode != 200) {
                         throw new IOException("Errore nel recupero cestino: " + statusCode);
                    }
                    
                    String json = EntityUtils.toString(response.getEntity());
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                    JsonObject list = root.getAsJsonObject("list");
                    JsonArray entries = list.getAsJsonArray("entries");
                    JsonObject pagination = list.getAsJsonObject("pagination");

                    for (JsonElement el : entries) {
                        JsonObject entry = el.getAsJsonObject().getAsJsonObject("entry");
                        if (entry.has("content")) {
                            JsonObject content = entry.getAsJsonObject("content");
                            if (content.has("sizeInBytes")) {
                                totalSize += content.get("sizeInBytes").getAsLong();
                            }
                        }
                    }

                    if (callback != null) {
                        callback.onProgress("Calcolo dimensione... elementi analizzati: " + (skipCount + entries.size()));
                    }

                    hasMore = pagination.get("hasMoreItems").getAsBoolean();
                    skipCount += entries.size();
                }
            }
        }
        return totalSize;
    }

    public void emptyTrashcan(ProgressCallback callback) throws Exception {
        // Try the legacy API first which clears everything: DELETE /alfresco/service/api/archive/workspace/SpacesStore
        String legacyUrl = normalizeBaseUrl(serverUrl) + "/service/api/archive/workspace/SpacesStore";
        
        try (CloseableHttpClient client = createHttpClient()) {
             if (callback != null) callback.onProgress("Tentativo svuotamento rapido (API Legacy)...");
             
             HttpDelete delete = new HttpDelete(legacyUrl);
             try (CloseableHttpResponse response = client.execute(delete)) {
                 int code = response.getStatusLine().getStatusCode();
                 if (code >= 200 && code < 300) {
                     if (callback != null) callback.onProgress("Cestino svuotato con successo!");
                     return;
                 }
             }
             
             // If legacy fails (e.g. 404), fall back to iterative delete (slower)
             if (callback != null) callback.onProgress("API Legacy non disponibile. Svuotamento iterativo...");
             emptyTrashcanIterative(client, callback);
        }
    }
    
    private void emptyTrashcanIterative(CloseableHttpClient client, ProgressCallback callback) throws Exception {
        String apiUrl = normalizeBaseUrl(serverUrl) + "/api/-default-/public/alfresco/versions/1/trashcan";
        boolean hasMore = true;
        
        while (hasMore) {
            if (callback != null && callback.isCancelled()) break;
            
            // Get a batch
            HttpGet get = new HttpGet(apiUrl + "?maxItems=50");
            JsonArray entries;
            
            try (CloseableHttpResponse response = client.execute(get)) {
                 String json = EntityUtils.toString(response.getEntity());
                 JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                 JsonObject list = root.getAsJsonObject("list");
                 entries = list.getAsJsonArray("entries");
                 if (entries.size() == 0) {
                     hasMore = false;
                     break;
                 }
            }
            
            // Delete batch
            for (JsonElement el : entries) {
                if (callback != null && callback.isCancelled()) break;
                String id = el.getAsJsonObject().getAsJsonObject("entry").get("id").getAsString();
                HttpDelete delete = new HttpDelete(apiUrl + "/" + id);
                client.execute(delete).close();
            }
            
            if (callback != null) callback.onProgress("Eliminati " + entries.size() + " elementi...");
        }
        if (callback != null) callback.onProgress("Finito.");
    }

    private String normalizeBaseUrl(String url) {
        // Strip /alfresco... if present to get base host, then add /alfresco
        // Actually usually user inputs http://host:port/alfresco
        if (url.endsWith("/api/-default-/public/cmis/versions/1.1/atom")) {
            return url.replace("/api/-default-/public/cmis/versions/1.1/atom", "");
        }
        // Assume user entered http://host:port/alfresco
        return url.replaceAll("/$", ""); 
    }
    
    public interface ProgressCallback {
        void onProgress(String message);
        boolean isCancelled();
    }
}
