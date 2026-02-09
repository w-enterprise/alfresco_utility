# Alfresco-Estrai-Nodi

Applicazione desktop Java per l'estrazione massiva di Node ID da cartelle Alfresco tramite protocollo CMIS.

## Requisiti

*   **Java Runtime Environment (JRE) 8** o superiore installato sul sistema.

## Installazione e Avvio

L'applicazione è distribuita come file JAR eseguibile. Non è necessaria un'installazione:

1.  Assicurati di avere il file `Alfresco-Estrai-Nodi-1.0-SNAPSHOT-jar-with-dependencies.jar`.
2.  Fai doppio clic sul file JAR per avviarlo (se l'associazione dei file .jar è configurata).
3.  In alternativa, apri un terminale e avvia l'applicazione con il comando:
    ```bash
    java -jar Alfresco-Estrai-Nodi-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```

## Guida all'Uso

All'avvio, l'applicazione presenta una finestra con i seguenti campi:

### 1. Configurazione Connessione
*   **CMIS URL**: L'indirizzo del servizio CMIS AtomPub di Alfresco.
    *   Default: `http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom`
*   **Username**: Il nome utente per l'accesso ad Alfresco.
*   **Password**: La password dell'utente.

### 2. Parametri di Estrazione
*   **Folder Node ID**: L'identificativo univoco (Node ID) della cartella Alfresco da cui si vogliono estrarre i contenuti (es. file e sottocartelle).
*   **Max items**: Il numero massimo di elementi da estrarre.
    *   Imposta `0` per estrarre **tutti** gli elementi presenti nella cartella (senza limiti).

### 3. Esecuzione
1.  Clicca sul pulsante **"Estrai Contenuti"**.
2.  La barra di progresso indicherà che l'elaborazione è in corso.
3.  L'area di log in basso mostrerà i dettagli delle operazioni (connessione, nodi trovati, tempo impiegato).

### 4. Output
Al termine dell'elaborazione, verrà generato un file CSV contenente l'elenco dei nodi estratti.
*   **Posizione**: Cartella `Downloads` del tuo utente.
*   **Nome file**: `alfresco_contents_[YYYYMMDD_HHmmss].csv`
*   **Formato CSV**: `Node ID,Name`

## Funzionalità Aggiuntive

*   **Salvataggio Automatico**: Le impostazioni inserite (URL, credenziali, ultimo Node ID usato, Max items) vengono salvate automaticamente in un file `alfresco_extractor_config.properties` nella stessa directory dell'eseguibile e ricaricate al successivo avvio.
*   **Log**: L'interfaccia mostra in tempo reale i nodi che vengono trovati e il tempo totale di esecuzione.
