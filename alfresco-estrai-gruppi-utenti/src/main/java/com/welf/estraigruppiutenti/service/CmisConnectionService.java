package com.welf.estraigruppiutenti.service;

import com.welf.estraigruppiutenti.model.CmisConnectionException;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisPermissionDeniedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servizio per la creazione della sessione CMIS verso Alfresco tramite OpenCMIS.
 * Implementa ottimizzazioni come il connection pooling tramite Apache HTTP Client.
 */
public class CmisConnectionService {
    private static final Logger logger = LoggerFactory.getLogger(CmisConnectionService.class);

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    /**
     * Crea un servizio con timeout di default.
     */
    public CmisConnectionService() {
        this(15_000, 60_000);
    }

    /**
     * Crea un servizio con timeout personalizzati.
     *
     * @param connectTimeoutMillis timeout di connessione in millisecondi.
     * @param readTimeoutMillis    timeout di lettura in millisecondi.
     */
    public CmisConnectionService(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    /**
     * Crea una sessione CMIS e verifica l'accesso al repository.
     * Utilizza il metodo di connessione AtomPub per default, compatibile con Alfresco.
     *
     * @param url      URL CMIS (AtomPub).
     * @param username username.
     * @param password password.
     * @return sessione CMIS.
     * @throws CmisConnectionException in caso di errore di connessione, autenticazione o timeout.
     */
    public Session createSession(String url, String username, String password) throws CmisConnectionException {
        String trimmedUrl = url == null ? "" : url.trim();
        if (trimmedUrl.isBlank()) {
            throw new CmisConnectionException("URL CMIS mancante.");
        }

        // Normalizzazione URL: aggiunge http e il path CMIS se mancanti
        String finalUrl = trimmedUrl;
        if (!finalUrl.toLowerCase().startsWith("http")) {
            finalUrl = "http://" + finalUrl;
        }
        
        if (!finalUrl.contains("/api/") && !finalUrl.contains("/cmis")) {
            if (!finalUrl.endsWith("/")) finalUrl += "/";
            if (!finalUrl.contains("/alfresco/")) finalUrl += "alfresco/";
            finalUrl += "api/-default-/public/cmis/versions/1.1/atom";
        }
        
        final String connectionUrl = finalUrl; // Variabile final per i log e catch
        logger.info("Tentativo di connessione CMIS a: {}", connectionUrl);

        if (username == null || username.trim().isEmpty()) {
            throw new CmisConnectionException("Username mancante.");
        }

        Map<String, String> parameter = new HashMap<>();
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password == null ? "" : password);
        
        // Timeout
        parameter.put(SessionParameter.CONNECT_TIMEOUT, String.valueOf(connectTimeoutMillis));
        parameter.put(SessionParameter.READ_TIMEOUT, String.valueOf(readTimeoutMillis));

        // Parametri standard Alfresco come in alfresco-export-folder-data
        parameter.put(SessionParameter.ATOMPUB_URL, connectionUrl);
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        parameter.put(SessionParameter.LOCALE_ISO3166_COUNTRY, "");
        parameter.put(SessionParameter.LOCALE_ISO639_LANGUAGE, "en");

        // Ottimizzazioni performance
        parameter.put(SessionParameter.HTTP_INVOKER_CLASS, "org.apache.chemistry.opencmis.client.bindings.spi.http.ApacheClientHttpInvoker");
        parameter.put(SessionParameter.COMPRESSION, "true");
        parameter.put(SessionParameter.CACHE_SIZE_OBJECTS, "1000");

        try {
            SessionFactory factory = SessionFactoryImpl.newInstance();
            List<Repository> repositories = factory.getRepositories(parameter);
            if (repositories == null || repositories.isEmpty()) {
                throw new CmisConnectionException("Nessun repository trovato a " + connectionUrl + ". Verifica che l'endpoint CMIS AtomPub sia corretto.");
            }
            
            Session session = repositories.get(0).createSession();
            logger.info("Sessione CMIS creata correttamente. Repository: {}", session.getRepositoryInfo().getName());
            
            return session;

        } catch (CmisUnauthorizedException e) {
            throw new CmisConnectionException("Autenticazione fallita (401). Verifica username/password.", e);
        } catch (CmisPermissionDeniedException e) {
            throw new CmisConnectionException("Permesso negato (403). L'utente non ha accesso al CMIS.", e);
        } catch (org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException e) {
            String detail = "";
            if (e.getCause() != null) {
                detail = " (Causa: " + e.getCause().getMessage() + ")";
            }
            throw new CmisConnectionException("Impossibile raggiungere Alfresco su " + connectionUrl + detail, e);
        } catch (CmisObjectNotFoundException e) {
            throw new CmisConnectionException("Endpoint CMIS non trovato (404) su " + connectionUrl, e);
        } catch (CmisRuntimeException e) {
            String msg = handleAlfrescoError(e);
            throw new CmisConnectionException("Errore runtime Alfresco: " + msg, e);
        } catch (CmisBaseException e) {
            throw new CmisConnectionException("Errore CMIS (" + e.getExceptionName() + "): " + safeMessage(e), e);
        } catch (Exception e) {
            throw new CmisConnectionException("Errore di connessione imprevisto: " + safeMessage(e), e);
        }
    }

    /**
     * Tenta di estrarre informazioni utili dagli errori runtime di Alfresco.
     */
    private String handleAlfrescoError(CmisRuntimeException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("re-authenticate")) {
            return "La sessione è scaduta o richiede una nuova autenticazione.";
        }
        return safeMessage(e);
    }

    private static String safeMessage(Throwable t) {
        if (t == null) return "Errore sconosciuto";
        String msg = t.getMessage();
        return msg == null ? t.getClass().getSimpleName() : msg;
    }
}
