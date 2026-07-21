# Alfresco Proprieta Cartelle

Applicazione desktop Java per analizzare il repository Alfresco e generare un report testuale contenente esclusivamente:
- cartelle con regole configurate
- file non associati alla classe documentale standard

## Funzionalità Principali

1. **Connessione ad Alfresco**: Tramite CMIS AtomPub
2. **Esplorazione Nodi**: Visualizzazione ad albero delle cartelle di Alfresco
3. **Selezione Multipla**: Seleziona più cartelle da analizzare
4. **Generazione Report**: Crea report TXT leggibili per ogni cartella selezionata con le sole categorie richieste
5. **Profondità Configurabile**: Imposta la profondità di analisi ricorsiva
6. **Log Operazioni**: Visualizzazione dettagliata delle operazioni in corso

## Prerequisiti

- Java 8 o superiore
- Maven (per compilare)
- Alfresco con CMIS abilitato

## Compilazione

```bash
mvn clean package
```

Questo creerà un file JAR shaded nella cartella `target/` con tutte le dipendenze incluse.

## Distribuzione Windows (EXE portabile)

Per creare una distribuzione Windows portabile (con runtime incluso) utilizzare lo script PowerShell:

```powershell
.\build-windows-package.ps1
```

Output:
- `dist\windows\Alfresco-Proprieta-Cartelle\Alfresco-Proprieta-Cartelle.exe`
- `dist\Alfresco-Proprieta-Cartelle.zip`

## Esecuzione

### Tramite Maven
```bash
mvn exec:java -Dexec.mainClass="it.welf.alfresco.folderprops.App"
```

### Tramite JAR
```bash
java -jar target/Alfresco-Proprieta-Cartelle.jar
```

## Configurazione

La connessione ad Alfresco viene configurata tramite l'interfaccia grafica. Le impostazioni vengono salvate automaticamente in `alfresco_folderprops_config.properties`.

### Parametri di Connessione
- **Indirizzo**: IP e porta di Alfresco (es: `localhost:8080` o `https://alfresco.example.com`)
- **Utente**: Nome utente Alfresco (es: `admin`)
- **Password**: Password dell'utente

## Struttura del Report TXT

Il report generato contiene due sezioni:

1. **Cartelle con regole configurate**
2. **File non associati alla classe documentale standard**

Per ogni elemento vengono riportati almeno nome, percorso completo e nodeId.
Per i file non standard vengono riportati anche MIME type, classe documentale e ultima modifica.

## Note Importanti

- Le cartelle vengono incluse solo se dispongono di regole attive/configurate
- I file vengono inclusi solo se il loro tipo documentale non rientra nelle classi standard Alfresco/CMIS
- L'applicazione genera esclusivamente file `.txt` per questa funzionalità
- La cache CMIS è disabilitata per garantire dati sempre aggiornati
