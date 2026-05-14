# Report Architettura CMIS Alfresco

## 1. Architettura di Comunicazione
L'applicazione utilizza la libreria **Apache Chemistry OpenCMIS** (versione 1.1.0) per interagire con Alfresco. 
La connessione avviene direttamente verso gli endpoint CMIS pubblici di Alfresco (`/api/-default-/public/cmis/versions/1.1/browser` o `/atom`).

### Flusso di Autenticazione
1. **Inizializzazione**: L'utente inserisce URL, Username e Password.
2. **Configurazione Sessione**: Viene creata una mappa di parametri che include:
   - `SessionParameter.USER` / `PASSWORD`
   - `SessionParameter.BINDING_TYPE`: Derivato automaticamente dall'URL (Browser Binding se contiene `/browser`, altrimenti AtomPub).
   - `SessionParameter.HTTP_INVOKER_CLASS`: Configurato per utilizzare **ApacheClientHttpInvoker** per abilitare il connection pooling.
3. **Handshake**: La `SessionFactory` interroga l'endpoint per ottenere la lista dei repository. Questo passaggio valida le credenziali tramite Basic Auth.
4. **Creazione Sessione**: Viene selezionato il primo repository disponibile e creata una sessione persistente.

### Ottimizzazioni Implementate
- **Connection Pooling**: Passaggio dall'invoker di default (HttpURLConnection) a **Apache HTTP Client**. Questo permette il riuso delle connessioni TCP/SSL, riducendo drasticamente la latenza su chiamate multiple.
- **Compressione**: Abilitata la compressione GZIP (`SessionParameter.COMPRESSION`) per ridurre il payload trasferito.
- **Caching**: Configurato un `CACHE_SIZE_OBJECTS` di 1000 per minimizzare le richieste di fetch degli stessi oggetti CMIS.
- **Timeout**: Impostati timeout di connessione (15s) e lettura (60s) più robusti per gestire carichi elevati su Alfresco.

## 2. Gestione Errori Alfresco
Alfresco può restituire risposte HTTP non standard o messaggi di errore complessi nel body. 
Il sistema è stato potenziato per:
- Intercettare `CmisRuntimeException` e analizzare il messaggio per identificare sessioni scadute.
- Gestire specificamente errori di connessione (`CmisConnectionException`) e di endpoint non trovati (404).
- Fornire messaggi chiari per errori di autenticazione (401) e permessi (403).

## 3. Conformità CMIS 1.0/1.1
L'implementazione supporta entrambi gli standard:
- **AtomPub**: Compatibile con CMIS 1.0 e 1.1.
- **Browser Binding (JSON)**: Introdotto con CMIS 1.1, è l'endpoint raccomandato per Alfresco in quanto più performante.

### Limitazioni ed Estensioni Alfresco
- **Proprietà Proprietarie**: Alfresco espone proprietà extra (es. `alfcmis:nodeRef`, `alfcmis:aspects`) che non fanno parte dello standard CMIS puro ma sono accessibili tramite la sessione OpenCMIS.
- **Aspects**: La gestione degli aspetti di Alfresco richiede l'uso di estensioni CMIS (aggiunta di tipi secondari).
- **Permissions**: Alfresco mappa le sue ACL complesse sulle ACL CMIS semplificate, il che può portare a discrepanze nella visualizzazione dei permessi granulari.

## 4. Test e Validazione
Sono stati implementati test unitari per validare:
- **Creazione Documenti**: Verifica del corretto invio delle proprietà e dello stream di contenuto.
- **Lettura**: Recupero tramite path.
- **Aggiornamento**: Sostituzione dello stream di contenuto su documenti esistenti.
- **Eliminazione**: Rimozione definitiva degli oggetti.

I test utilizzano **Mockito** per simulare l'ambiente Alfresco e garantire che il codice di integrazione rispetti le specifiche del protocollo.
