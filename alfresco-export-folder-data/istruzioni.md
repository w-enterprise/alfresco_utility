# Istruzioni d’uso (Interfaccia Utente) — Alfresco Export Folder Data

Questo documento descrive in modo operativo e dettagliato l’utilizzo dell’applicazione **esclusivamente tramite l’interfaccia utente**: elementi disponibili, parametri configurabili, vincoli, interdipendenze ed esempi di utilizzo.

## 1. Panoramica UI (mappa a schermo)

L’interfaccia è composta da quattro aree principali:

1. **Connessione Alfresco (in alto)**: parametri di accesso e pulsante di connessione.
2. **Esplora e Seleziona un Nodo (sinistra/alto)**: albero dei nodi per selezionare una cartella.
3. **Log Operazioni (sinistra/basso)**: output testuale delle operazioni (caricamento, esportazione, errori).
4. **Opzioni Esportazione (destra)**: parametri di export e comandi.

### Riferimento visivo (screenshot)

- **Screenshot consigliato**: schermata principale dopo la connessione, con una cartella selezionata nell’albero e le opzioni di esportazione visibili.
- **Annotazione consigliata**: evidenziare con numeri (1–4) le aree sopra e con lettere (A–H) i controlli descritti nelle sezioni seguenti.

## 2. Sezione “Connessione Alfresco” (in alto)

Questa sezione controlla l’accesso al repository Alfresco via CMIS.

### 2.1 Indirizzo (IP:Porta)
- **Etichetta UI**: `Indirizzo (IP:Porta):`
- **Tipo**: stringa
- **Formato atteso**:
  - `host:porta` (es. `localhost:8080`)
  - oppure URL completo con protocollo (es. `http://server:8080`)
- **Default**: `localhost:8080`
- **Note operative**:
  - Se non inserisci il protocollo (`http://`), l’app lo aggiunge automaticamente.
  - Il percorso CMIS viene aggiunto automaticamente all’indirizzo.

### 2.2 Utente
- **Etichetta UI**: `Utente:`
- **Tipo**: stringa
- **Default**: `admin`
- **Uso**: username Alfresco.

### 2.3 Password
- **Etichetta UI**: `Password:`
- **Tipo**: stringa (mascherata)
- **Default**: `admin`
- **Uso**: password Alfresco.

### 2.4 Pulsante “Connetti ed Esplora”
- **Etichetta UI**: `Connetti ed Esplora`
- **Azione**:
  - tenta la connessione
  - in caso di successo, carica l’albero dei nodi (radice + livelli iniziali)
  - abilita le funzioni che richiedono sessione (validazione nodo di partenza, esportazione)
- **Feedback**:
  - aggiorna la barra di stato (in basso)
  - scrive dettagli in “Log Operazioni”
  - in caso di errore mostra un dialog con il messaggio di errore.

## 3. Sezione “Esplora e Seleziona un Nodo” (albero)

### 3.1 Albero nodi
- **Elemento UI**: albero (`JTree`)
- **Uso**:
  - espandi e seleziona una cartella per impostarla come nodo di partenza “selezionato da UI”
- **Effetti collaterali della selezione**:
  - aggiorna i campi informativi in “Opzioni Esportazione”:
    - `Cartella Selezionata`
    - `Node-ID`
  - copia nel campo “Nodo di partenza (path o nodeId)” il path del nodo selezionato (se disponibile)
  - se avevi precedentemente impostato un “Nodo di partenza” manuale tramite validazione, la selezione nell’albero **lo annulla** e torna a usare il nodo selezionato nell’albero.

## 4. Sezione “Log Operazioni” (output testuale)

### 4.1 Log Operazioni
- **Elemento UI**: area di testo non editabile
- **Contenuto**:
  - messaggi progressivi delle operazioni:
    - caricamento nodi
    - validazione nodo
    - progressione export
    - messaggi di stop richiesto
    - errori applicativi
- **Uso**:
  - utile per capire “cosa sta succedendo” durante operazioni lunghe
  - in caso di errore, spesso contiene un messaggio più descrittivo rispetto alla barra di stato.

## 5. Sezione “Opzioni Esportazione” (pannello destro)

### 5.1 Cartella Selezionata
- **Etichetta UI**: `Cartella Selezionata:`
- **Tipo**: label
- **Valore**:
  - il nome della cartella selezionata nell’albero **oppure** della cartella validata nel campo “Nodo di partenza”
- **Default**: `-`

### 5.2 Node-ID
- **Etichetta UI**: `Node-ID:`
- **Tipo**: label
- **Valore**:
  - NodeId della cartella selezionata/validata
- **Default**: `-`

#### Pulsante “Copia”
- **Etichetta UI**: `Copia`
- **Abilitazione**:
  - abilitato solo quando un nodo valido è selezionato o validato
- **Azione**:
  - copia il Node-ID negli appunti
- **Uso tipico**:
  - incollare il Node-ID nel campo “Nodo di partenza (path o nodeId)” su una successiva esecuzione/validazione.

### 5.3 Nodo di partenza (path o nodeId)
- **Etichetta UI**: `Nodo di partenza (path o nodeId):`
- **Tipo**: stringa
- **Valori ammessi**:
  - **Path Alfresco**: stringa che inizia con `/` (es. `/Sites/aqpimportazione/documentLibrary/2021`)
  - **Node-ID**: stringa non vuota (es. `2da19566-d80c-4b78-9623-92b27c60df25`)
- **Default**: vuoto
- **Scopo**:
  - definisce manualmente il nodo da cui partire, senza doverlo cercare nell’albero.

#### Pulsante “Valida”
- **Etichetta UI**: `Valida`
- **Abilitazione**:
  - abilitato solo dopo una connessione riuscita
- **Azione** (step-by-step):
  1. legge il valore del campo “Nodo di partenza (path o nodeId)”
  2. se inizia con `/` lo interpreta come **path**
  3. altrimenti lo interpreta come **node-id**
  4. verifica che il nodo esista e sia una **cartella** accessibile
  5. se valido:
     - aggiorna `Cartella Selezionata` e `Node-ID`
     - se è disponibile un path, lo riscrive nel campo “Nodo di partenza…” (normalizzazione)
     - abilita l’export
     - aggiorna il “Nome File CSV” automaticamente solo se non è stato personalizzato manualmente
  6. se non valido:
     - mostra un messaggio di errore
     - mantiene disabilitata l’esportazione

**Casi d’uso tipici**
- “Ho solo il node-id”: incolla il node-id e premi **Valida** per ottenere anche path e nome cartella.
- “Ho un path completo”: incolla il path e premi **Valida**.

### 5.4 Profondità (0=tutti)
- **Etichetta UI**: `Profondità (0=tutti):`
- **Tipo**: intero (spinner)
- **Range**: 0–100
- **Default**: 0
- **Semantica**:
  - `0` = nessun limite: visita tutta la sotto-struttura
  - `1` = elabora la cartella di partenza e i suoi figli diretti
  - `2` = include anche i nipoti, ecc.
- **Interdipendenze**:
  - il valore influenza anche il naming automatico del file (se non hai “sporcato” manualmente il campo Nome File CSV).

### 5.5 Nome File CSV
- **Etichetta UI**: `Nome File CSV:`
- **Tipo**: stringa
- **Default**: un valore iniziale generico (alla prima apertura) e poi aggiornato automaticamente in base al nodo (se non personalizzato)

#### Regola di sincronizzazione (flag “sporco”)
Il campo è gestito con un flag di stato interno:
- **Mai modificato manualmente**:
  - quando selezioni/validi un nodo, l’app aggiorna automaticamente il nome suggerito.
  - quando l’app crea il file finale, può aggiornare il campo per mostrare il nome effettivo.
- **Modificato manualmente almeno una volta** (“campo sporco”):
  - l’app **non sovrascrive più** il campo con valori automatici.
  - l’app usa **esattamente** il valore inserito manualmente come nome destinazione in export (con validazioni).

#### Validazioni se il campo è “sporco”
Quando hai personalizzato manualmente il campo:
- non può essere vuoto
- non può contenere caratteri non validi per Windows (`\\ / : * ? \" < > |`)
- se non termina con `.csv`, l’app aggiunge automaticamente l’estensione
- se il file esiste già, l’app blocca l’export per evitare sovrascrittura

#### Formato del nome automatico (quando il campo NON è “sporco”)
Quando l’app genera automaticamente il nome:
- **Formato**:
  - `[nome_cartella_nodo]_[YYYYMMDD]_[HHMMSS]_[nodeIdShort]_d[profondità].csv`
- **Note**:
  - `nome_cartella_nodo` viene sanitizzato per evitare caratteri non validi.
  - `nodeIdShort` è una porzione breve del node-id (per riconoscibilità).
  - in caso di conflitto sul filesystem, viene aggiunto un suffisso incrementale `_1`, `_2`, ...

### 5.6 Pulsante “Avvia Esportazione”
- **Etichetta UI**: `Avvia Esportazione`
- **Abilitazione**:
  - richiede una connessione attiva
  - richiede un nodo valido selezionato nell’albero oppure validato nel campo “Nodo di partenza”
- **Nodo di partenza usato**:
  - se hai validato un nodo tramite “Nodo di partenza…”, l’export parte da quello
  - altrimenti parte dalla cartella selezionata nell’albero
- **Output**:
  - produce un CSV nel formato definito dall’applicazione
  - scrive avanzamento nel “Log Operazioni”
  - aggiorna la barra di stato

### 5.7 Pulsante “Interrompi Esportazione”
- **Etichetta UI**: `Interrompi Esportazione`
- **Abilitazione**: solo durante un export in corso
- **Azione**:
  - richiede l’interruzione “cooperativa” dell’elaborazione
  - l’export termina appena raggiunge un punto di controllo
- **Effetto sui dati**:
  - il CSV può risultare **parziale**, ma viene chiuso e salvato con quanto già scritto
- **Feedback**:
  - messaggio nel log (`[STOP] ...`)
  - dialog di conferma con percorso del file salvato parzialmente.

## 6. Procedure operative passo‑passo

### 6.1 Esportare partendo da una cartella selezionata nell’albero
1. Connettersi con “Connetti ed Esplora”.
2. Nell’albero selezionare la cartella desiderata.
3. Impostare la “Profondità”.
4. (Opzionale) Personalizzare “Nome File CSV”:
   - attenzione: appena modifichi manualmente, il campo diventa “sporco”.
5. Cliccare “Avvia Esportazione”.

### 6.2 Esportare partendo da un path noto
1. Connettersi con “Connetti ed Esplora”.
2. Nel campo “Nodo di partenza (path o nodeId)” incollare un path che inizi con `/`.
3. Cliccare “Valida”.
4. Verificare che `Cartella Selezionata` e `Node-ID` siano valorizzati.
5. Cliccare “Avvia Esportazione”.

### 6.3 Esportare partendo da un node-id noto (e ricavare il path)
1. Connettersi con “Connetti ed Esplora”.
2. Incollare il node-id nel campo “Nodo di partenza (path o nodeId)”.
3. Cliccare “Valida”.
4. Se disponibile, il campo “Nodo di partenza…” viene popolato con il path del nodo.
5. Cliccare “Avvia Esportazione”.

### 6.4 Interrompere un’esportazione lunga e salvare il parziale
1. Avviare l’esportazione.
2. Cliccare “Interrompi Esportazione”.
3. Attendere la terminazione cooperativa.
4. Recuperare il percorso del CSV parziale dal dialog finale.

## 7. Esempi pratici (scenari comuni)

### Scenario A — “Uso rapido da GUI”
- Seleziona nell’albero `/Sites/.../documentLibrary/2021`
- Profondità: `0`
- Nome CSV: non modificare
- Risultato tipico:
  - `2021_20260506_142514_2da19566_d0.csv`

### Scenario B — “Parto da node-id ricevuto via ticket”
- Nodo di partenza: `2da19566-d80c-4b78-9623-92b27c60df25`
- Clic “Valida”
- Profondità: `2`
- Risultato tipico:
  - `[nomeCartella]_YYYYMMDD_HHMMSS_[nodeIdShort]_d2.csv`

### Scenario C — “Nome file personalizzato definitivo”
- Modifica manualmente “Nome File CSV” in `export_cliente_X.csv`
- Nota: da questo momento l’app non lo aggiorna più automaticamente
- Se esiste già, l’export viene bloccato per evitare sovrascrittura

