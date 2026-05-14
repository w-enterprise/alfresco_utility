# Alfresco - Estrai Gruppi e Utenti

Questa applicazione permette di connettersi a un repository Alfresco tramite il protocollo CMIS per visualizzare i gruppi presenti ed esportare l'elenco dei relativi utenti membri in formato CSV.

## Istruzioni per l'Uso

### 1. Avvio dell'Applicazione
Eseguire il file `Alfresco-Estrai-Gruppi-Utenti.exe` contenuto nella cartella dell'applicazione.

### 2. Connessione al Repository
Inserire i parametri richiesti nel pannello superiore:
- **URL CMIS**: L'indirizzo del servizio AtomPub di Alfresco.
  - *Esempio*: `http://172.22.5.118:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom`
  - *Nota*: Se si inserisce solo l'indirizzo IP (es. `172.22.5.118:8080`), l'applicazione proverà a completare l'URL automaticamente.
- **Username**: Il proprio nome utente Alfresco (es. `admin`).
- **Password**: La password associata all'utente.

Premere il pulsante **Connetti**. Al termine della connessione, la tabella sottostante verrà popolata automaticamente con tutti i gruppi trovati nel sistema.

### 3. Gestione dei Gruppi
Nella tabella centrale è possibile:
- **Visualizzare**: Nome del gruppo, descrizione e numero di utenti membri diretti.
- **Selezionare**: Cliccare sulla casella di controllo a sinistra di ogni riga.
- **Seleziona Tutto**: Cliccare sulla casella di controllo nell'intestazione della colonna per selezionare o deselezionare tutti i gruppi contemporaneamente.
- **Aggiornare**: Premere il pulsante **Aggiorna** per ricaricare la lista dei gruppi dal server.

### 4. Esportazione Dati
Dopo aver selezionato almeno un gruppo:
1. Premere il pulsante **Esporta**.
2. L'applicazione inizierà a recuperare i membri di ogni gruppo selezionato.
3. Al termine, verrà generato un file CSV nella stessa cartella dell'applicazione, con nome `gruppi_utenti_alfresco_YYYYMMDD_HHMMSS.csv`.

---

## Formato del File CSV
Il file generato utilizza il punto e virgola (`;`) come separatore e contiene le seguenti colonne:
- **gruppo**: Nome visualizzato del gruppo Alfresco.
- **utente**: Username dell'utente membro.
- **ripetuto**: Contiene un asterisco (`*`) se l'utente è già stato elencato in un gruppo precedente (utile per identificare utenti presenti in più gruppi).

---

## Diagnostica e Log
Il pannello inferiore **Log Attività** mostra in tempo reale le operazioni in corso e l'esito delle connessioni. In caso di errore, i dettagli tecnici verranno visualizzati in quest'area per facilitare la risoluzione dei problemi.

## Note Tecniche
- L'applicazione richiede una connessione di rete verso il server Alfresco sulla porta specificata (default 8080).
- Il tempo di esportazione dipende dal numero di gruppi selezionati e dalla velocità di risposta del server. È possibile interrompere l'operazione in qualsiasi momento premendo il pulsante **Interrompi**.
