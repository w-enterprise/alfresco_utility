# Alfresco-Cancella-Nodi - Guida all'uso

## Descrizione
Applicazione desktop per la cancellazione massiva di nodi da Alfresco tramite protocollo CMIS, partendo da un elenco di ID forniti via CSV.

## Requisiti di Sistema
- **Java Runtime Environment (JRE)** installato.
  - L'applicazione supporta sia Java 8 che Java 11 (o versioni successive).

## Come Avviare l'Applicazione
Nella cartella principale sono presenti due script di avvio. Utilizza quello corrispondente alla versione di Java installata sul tuo computer:

1. **Per Java 11 o superiore (Consigliato)**:
   - Esegui `run.bat`
   - Questo script utilizza la versione "moderna" dell'applicazione con le librerie JavaFX incluse.

2. **Per Java 8**:
   - Esegui `run-java8.bat`
   - Questo script utilizza la versione compatibile con Java 8.

## Configurazione
All'avvio, compila i campi per la connessione al server Alfresco:
- **Alfresco CMIS URL**: L'indirizzo del servizio CMIS AtomPub. 
  - Esempio: `http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom`
- **Username**: Utente con permessi di cancellazione sui nodi target.
- **Password**: Password dell'utente.

> **Nota**: I parametri di connessione vengono salvati automaticamente in forma cifrata nel file `config.properties` per facilitare i futuri utilizzi.

## Formato del File CSV
Il file CSV di input deve rispettare questo formato semplice:
- **Colonna 1**: Deve contenere l'ID del nodo (UUID o CMIS Object ID).
- **Separatore**: Sono supportati sia la virgola (`,`) che il punto e virgola (`;`).
- L'applicazione tenterà di leggere la prima colonna di ogni riga.

## Funzionalità Principali

### 1. Seleziona File CSV
Clicca sul pulsante "Seleziona File CSV" per caricare la lista dei nodi da processare.

### 2. Limite Nodi (Opzionale)
Nel campo "Limite Nodi", puoi inserire un numero intero (es. `50`).
- L'applicazione si fermerà dopo aver processato con successo (o tentato di cancellare) quel numero di nodi.
- I nodi non trovati o non validi **non** vengono conteggiati nel limite.

### 3. Modalità TEST (Simulazione)
Clicca sul pulsante **TEST** per eseguire una verifica senza modificare nulla sul server.
- L'applicazione verificherà per ogni ID se il nodo esiste ed è accessibile.
- Viene generato un report finale.
- **Uso consigliato**: Esegui sempre un TEST prima di procedere alla cancellazione.

### 4. Modalità CANCELLA
Clicca sul pulsante **CANCELLA** per rimuovere definitivamente i nodi.
- Verrà richiesta una conferma esplicita.
- I nodi verranno eliminati permanentemente dal repository Alfresco.
- Viene generato un report finale.

## Report e Log
Al termine di ogni operazione (Test o Cancellazione), viene generato un file di report nella stessa cartella del file CSV originale.
- Nome del file: `[NomeFileOriginale]_report_[DataOra].csv`
- Il report contiene l'esito per ogni singolo nodo (es. `TROVATO`, `NON TROVATO`, `CANCELLATO`, `ERRORE`) e eventuali messaggi di dettaglio.

L'interfaccia mostra anche un log in tempo reale delle operazioni e una barra di avanzamento.
