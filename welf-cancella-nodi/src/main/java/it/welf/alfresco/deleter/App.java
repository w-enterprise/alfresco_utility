package it.welf.alfresco.deleter;

import it.welf.alfresco.deleter.model.ConfigManager;
import it.welf.alfresco.deleter.model.NodeInfo;
import it.welf.alfresco.deleter.service.AlfrescoService;
import it.welf.alfresco.deleter.service.CsvService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class App extends Application {

    private TextField urlField;
    private TextField userField;
    private PasswordField passField;
    private Label fileLabel;
    private TextField limitField;
    private TextArea logArea;
    private ProgressBar progressBar;
    private Button testButton;
    private Button deleteButton;
    
    private File selectedFile;
    private final ConfigManager configManager = new ConfigManager();
    private final AlfrescoService alfrescoService = new AlfrescoService();
    private final CsvService csvService = new CsvService();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Alfresco-Cancella-Nodi");

        // Layout
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-font-family: 'Segoe UI', sans-serif;");

        // Connection Section
        TitledPane connectionPane = createConnectionPane();
        
        // File Section
        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        Button chooseFileBtn = new Button("Seleziona CSV...");
        chooseFileBtn.setOnAction(e -> chooseFile(primaryStage));
        fileLabel = new Label("Nessun file selezionato");
        fileBox.getChildren().addAll(chooseFileBtn, fileLabel);

        // Limit Section
        HBox limitBox = new HBox(10);
        limitBox.setAlignment(Pos.CENTER_LEFT);
        limitField = new TextField();
        limitField.setPromptText("Tutti");
        limitField.setPrefWidth(100);
        // Force numeric input
        limitField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                limitField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        limitBox.getChildren().addAll(new Label("Limite nodi da elaborare (vuoto = tutti):"), limitField);

        // Actions Section
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);
        testButton = new Button("TEST (Simulazione)");
        testButton.setStyle("-fx-base: #4a90e2; -fx-text-fill: white; -fx-font-weight: bold;");
        testButton.setOnAction(e -> runTask(true));
        
        deleteButton = new Button("CANCELLAZIONE DEFINITIVA");
        deleteButton.setStyle("-fx-base: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteButton.setOnAction(e -> confirmAndDelete());
        
        actionBox.getChildren().addAll(testButton, deleteButton);

        // Progress
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        
        // Log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(300);
        logArea.setStyle("-fx-font-family: 'Consolas', monospace;");

        root.getChildren().addAll(connectionPane, fileBox, limitBox, actionBox, progressBar, new Label("Log Operazioni:"), logArea);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Load Config
        loadConfig();
        
        // Disable buttons initially
        updateButtonState();
    }

    private TitledPane createConnectionPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        urlField = new TextField();
        urlField.setPromptText("http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom");
        urlField.setPrefWidth(400);
        
        userField = new TextField();
        passField = new PasswordField();

        grid.add(new Label("Alfresco CMIS URL:"), 0, 0);
        grid.add(urlField, 1, 0);
        grid.add(new Label("Username:"), 0, 1);
        grid.add(userField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passField, 1, 2);

        TitledPane pane = new TitledPane("Parametri di Connessione", grid);
        pane.setCollapsible(false);
        return pane;
    }

    private void chooseFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona File CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedFile = file;
            fileLabel.setText(file.getAbsolutePath());
            updateButtonState();
        }
    }

    private void updateButtonState() {
        boolean disabled = selectedFile == null;
        testButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
    }

    private void loadConfig() {
        ConfigManager.ConnectionConfig config = configManager.loadConfig();
        urlField.setText(config.getUrl());
        userField.setText(config.getUsername());
        passField.setText(config.getPassword());
    }

    private void saveConfig() {
        try {
            configManager.saveConfig(urlField.getText(), userField.getText(), passField.getText());
        } catch (Exception e) {
            log("Errore nel salvataggio della configurazione: " + e.getMessage());
        }
    }

    private void confirmAndDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Conferma Cancellazione");
        alert.setHeaderText("Attenzione: Operazione Irreversibile");
        alert.setContentText("Sei sicuro di voler cancellare definitivamente i nodi elencati nel CSV?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            runTask(false);
        }
    }

    private void log(String message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        Platform.runLater(() -> logArea.appendText("[" + time + "] " + message + "\n"));
    }

    private void runTask(boolean isTest) {
        String url = urlField.getText();
        String user = userField.getText();
        String pass = passField.getText();
        String limitStr = limitField.getText();
        
        final int limit;
        if (limitStr != null && !limitStr.isEmpty()) {
            limit = Integer.parseInt(limitStr);
        } else {
            limit = -1; // No limit
        }

        if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            log("Errore: Parametri di connessione mancanti.");
            return;
        }

        saveConfig();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    log("Connessione a " + url + "...");
                    alfrescoService.connect(url, user, pass);
                    log("Connesso con successo.");

                    log("Lettura CSV...");
                    List<String> ids = csvService.readNodeIds(selectedFile);
                    log("Trovati " + ids.size() + " ID nel CSV.");
                    
                    int maxToProcess = ids.size();
                    if (limit > 0 && limit < maxToProcess) {
                        maxToProcess = limit;
                        log("Limit impostato a: " + limit + " nodi.");
                    } else {
                        log("Elaborazione di tutti i nodi.");
                    }

                    List<NodeInfo> results = new ArrayList<>();
                    int count = 0;
                    int processedCount = 0;
                    int successCount = 0;

                    for (String id : ids) {
                        if (isCancelled()) break;
                        if (processedCount >= maxToProcess) break;
                        
                        NodeInfo info;
                        if (isTest) {
                            info = alfrescoService.checkNode(id);
                        } else {
                            info = alfrescoService.deleteNode(id);
                        }
                        
                        // Always add to results report
                        results.add(info);
                        
                        // Count as processed only if actually attempted (found or error during attempt)
                        // If "NOT_FOUND", we might still consider it processed or skipped depending on requirement.
                        // User said: "ovviamente salterà i nodi che non troverà" implies we don't count them towards the limit of DELETED nodes?
                        // Or simply iterate until we hit the limit of attempts? 
                        // Standard interpretation: Process N rows from file.
                        // But user said "salterà i nodi che non troverà" in context of "choose how many nodes to delete".
                        // This suggests we should keep going until we have successfully deleted (or checked) N nodes.
                        
                        boolean isNotFound = "NOT_FOUND".equals(info.getStatus());
                        
                        log((isTest ? "Check" : "Delete") + ": " + info.toString());

                        if (!isNotFound) {
                            processedCount++;
                            successCount++;
                        } else {
                            // If not found, we don't count it towards the limit of "deleted nodes"
                            // so we continue loop without incrementing processedCount
                            log("Nodo non trovato, salto e continuo...");
                        }
                        
                        count++; // Total rows iterated
                        updateProgress(processedCount, maxToProcess);
                    }

                    // Report generation
                    String suffix = isTest ? "_test_report.csv" : "_delete_report.csv";
                    String reportPath = selectedFile.getAbsolutePath().replace(".csv", suffix);
                    File reportFile = new File(reportPath);
                    csvService.writeReport(reportFile, results);
                    log("Report salvato in: " + reportPath);
                    
                    Platform.runLater(() -> {
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("Operazione Completata");
                        info.setHeaderText(null);
                        info.setContentText("Elaborazione terminata. Controlla il report.");
                        info.show();
                    });

                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    log("ERRORE FATALE: " + e.getMessage());
                    log(sw.toString());
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
