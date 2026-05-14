package com.welf.estraigruppiutenti.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Utility per derivare gli endpoint Alfresco a partire dall'URL CMIS.
 */
public class AlfrescoUrlUtils {
    private static final Pattern CMIS_ATOM_SUFFIX = Pattern.compile("/api/-default-/public/cmis/versions/\\d+\\.\\d+/atom/?$");
    private static final Pattern CMIS_BROWSER_SUFFIX = Pattern.compile("/api/-default-/public/cmis/versions/\\d+\\.\\d+/browser/?$");

    /**
     * Deriva l'URL base di Alfresco (es. http://host:port/alfresco) a partire dall'URL CMIS.
     *
     * @param cmisUrl URL CMIS inserito dall'utente.
     * @return URL base Alfresco.
     */
    public static String toAlfrescoBaseUrl(String cmisUrl) {
        if (cmisUrl == null) {
            return "";
        }
        String url = cmisUrl.trim();
        
        // Assicura la presenza del protocollo
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://")) {
            url = "http://" + url;
        }

        // Se l'utente ha inserito solo l'host, aggiungiamo /alfresco
        if (!url.contains("/alfresco") && (url.split("/").length <= 3)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += "alfresco";
        }

        url = CMIS_ATOM_SUFFIX.matcher(url).replaceFirst("");
        url = CMIS_BROWSER_SUFFIX.matcher(url).replaceFirst("");
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Codifica un segmento di path per inserirlo in un URL.
     *
     * @param segment segmento.
     * @return segmento codificato.
     */
    public static String encodePathSegment(String segment) {
        if (segment == null) {
            return "";
        }
        String encoded = java.net.URLEncoder.encode(segment, StandardCharsets.UTF_8);
        return encoded.replace("+", "%20");
    }
}
