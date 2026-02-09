# Alfresco-Ritorna-ParentFolder

Applicazione desktop per recuperare le informazioni del nodo padre (Parent Node) di un documento o cartella in Alfresco tramite protocollo CMIS.

## Requisiti

*   **Java Runtime Environment (JRE) 8** o superiore installato sul sistema.

## Installazione

Non è richiesta alcuna installazione specifica. È sufficiente scaricare l'applicazione e assicurarsi di avere Java installato.

## Come Avviare l'Applicazione

### Windows
Puoi avviare l'applicazione in due modi:
1.  **Eseguibile (.exe)**: Fai doppio click sul file `Alfresco-Ritorna-ParentFolder.exe`.
2.  **Script Batch (.bat)**: Fai doppio click sul file `Alfresco-Ritorna-ParentFolder.bat`.

### macOS / Linux
Apri il terminale, naviga nella cartella dell'applicazione ed esegui:

```bash
java -jar Alfresco-Ritorna-ParentFolder.jar
```

*(Nota: Assicurati che il file .jar sia presente nella directory. Se stai compilando dai sorgenti, il file si troverà nella cartella `target`)*.

## Come Usare

1.  **Configurazione Connessione**:
    All'avvio, compila i campi per la connessione ad Alfresco:
    *   **CMIS URL**: L'endpoint CMIS AtomPub di Alfresco.
        *   Esempio Alfresco 5.2+: `http://iltuoserver:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom`
        *   Esempio Alfresco 4.x: `http://iltuoserver:8080/alfresco/cmisatom`
    *   **Username**: Il tuo nome utente Alfresco.
    *   **Password**: La tua password.

2.  **Inserimento Node ID**:
    *   **Target Node ID**: Incolla l'UUID del nodo (documento o cartella) di cui vuoi trovare il genitore.

3.  **Ricerca**:
    *   Clicca sul pulsante **Find Parent Node**.

4.  **Risultato**:
    *   L'area di testo "Output / Log" mostrerà lo stato della connessione.
    *   Se l'operazione ha successo, vedrai i dettagli del **Parent Node**:
        *   **ID**: L'UUID della cartella padre.
        *   **Name**: Il nome della cartella padre.
        *   **Type**: Il tipo di oggetto (es. `cmis:folder`).

## Funzionalità Aggiuntive

*   **Salvataggio Configurazione**: Dopo una connessione riuscita, i parametri (URL, Username, Password) vengono salvati automaticamente in un file locale `alfresco_tool_config.properties`.
*   **Sicurezza**: La password viene salvata in modo cifrato (AES-128) nel file di configurazione.
*   **Log**: L'interfaccia fornisce feedback in tempo reale su errori di connessione o ID non validi.
