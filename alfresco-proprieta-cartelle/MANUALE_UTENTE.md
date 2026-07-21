# Manuale Utente - Alfresco Proprieta Cartelle

## Introduzione
Alfresco Proprieta Cartelle è un'applicazione desktop Java che consente di analizzare il repository Alfresco per individuare:
1. Le cartelle che dispongono di regole configurate
2. I file che non sono associati alla classe documentale standard

L'applicazione genera report TXT strutturati e leggibili con le sole categorie sopra indicate.

## Installazione
Nessuna installazione richiesta! L'applicazione è distribuita come file JAR eseguibile o come pacchetto precompilato.

### Requisiti
- Java 8 o versione superiore installato sul computer

## Avvio dell'Applicazione
1. Doppio clic sul file JAR o sull'eseguibile
2. Attendere il caricamento dell'interfaccia grafica

## Connessione ad Alfresco
1. Nel pannello "Connessione Alfresco" in alto:
   - Inserisci l'indirizzo di Alfresco (es: `localhost:8080` o `https://alfresco.tuoazienda.it`)
   - Inserisci il nome utente (es: `admin`)
   - Inserisci la password
2. Clicca sul pulsante **"Connetti ed Esplora"**
3. Attendi il caricamento dei nodi radice

## Esplorazione e Selezione Cartelle
1. Dopo la connessione, nella sezione "Esplora e Seleziona Nodi" verrà mostrato l'albero delle cartelle
2. Per selezionare una cartella:
   - Clicca sulla casella di spunta accanto al nome della cartella
   - Puoi selezionare più cartelle contemporaneamente
3. Per selezionare tutte le cartelle: clicca **"Seleziona Tutti"**
4. Per deselezionare tutte le cartelle: clicca **"Deseleziona Tutti"**

### Selezione Tramite Path o Node-ID
Se conosci già il percorso o l'ID della cartella:
1. Inserisci il percorso (es: `/Sites/mio-sito/documentLibrary`) o l'ID nel campo "Nodo di partenza"
2. Clicca **"Valida"**
3. Se valido, la cartella verrà selezionata automaticamente

## Configurazione Report
Nel pannello "Opzioni Report" sulla destra:
1. **Profondità**: Imposta la profondità di analisi ricorsiva (0 = analizza tutte le sottocartelle)
2. **Nome File Report**: Modifica il nome del file TXT di output (se lasciato vuoto, viene generato automaticamente)

## Generazione Report
1. Dopo aver selezionato le cartelle, clicca **"Genera Report"**
2. Monitora l'avanzamento nella barra di stato e nel pannello "Log Operazioni"
3. Per interrompere l'operazione: clicca **"Annulla Operazione"**
4. Al termine, i file TXT verranno salvati nella cartella dell'applicazione

## Struttura del Report TXT
Ogni report contiene due sezioni:

1. **Cartelle con regole configurate**
2. **File non associati alla classe documentale standard**

Per ogni cartella vengono riportati nome, percorso completo e nodeId.
Per ogni file vengono riportati nome, percorso completo, nodeId, MIME type, classe documentale e data di ultima modifica.

## Note Importanti
- Le credenziali di accesso vengono salvate automaticamente in un file di configurazione
- L'applicazione disabilita la cache CMIS per garantire dati sempre aggiornati
- I log delle operazioni vengono salvati nella cartella `logs/`
- Per cartelle molto grandi, l'analisi potrebbe richiedere diversi minuti
- I file conformi alla classe documentale standard vengono esclusi automaticamente dal report
- Questa funzionalità non genera file CSV
