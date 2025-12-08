package ch.ehealth.levi.gui.controller;

import ch.ehealth.levi.gui.model.AppConfig;
import ch.ehealth.levi.gui.model.JobResult;
import ch.ehealth.levi.gui.service.ConfigService;
import ch.ehealth.levi.gui.service.JobService;
import ch.ehealth.levi.gui.util.I18nUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import translation.check.Conf;
import translation.check.DbConnection;

import java.io.File;
import java.sql.Connection;
import java.time.Duration;

/**
 * Main controller for the LEVI GUI application
 */
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    // Services
    private final ConfigService configService;
    private final JobService jobService;
    
    // Stage
    private Stage stage;
    
    // Configuration Section
    @FXML private TextField dbUrlField;
    @FXML private TextField dbUsernameField;
    @FXML private PasswordField dbPasswordField;
    @FXML private Button dbTestButton;
    
    @FXML private ComboBox<String> countryCodeCombo;
    @FXML private CheckBox eszettCheckBox;
    @FXML private CheckBox regexCheckBox;
    
    @FXML private TextField currentFileField;
    @FXML private Button currentFileBrowseButton;
    @FXML private TextField previousFileField;
    @FXML private Button previousFileBrowseButton;
    @FXML private TextField outputDirField;
    @FXML private Button outputDirBrowseButton;
    
    @FXML private Button saveConfigButton;
    @FXML private Button loadConfigButton;
    @FXML private Button restoreDefaultsButton;
    
    // Jobs Section
    @FXML private Button overviewButton;
    @FXML private Button descAddButton;
    @FXML private Button descInactButton;
    @FXML private Button translateDeltaButton;
    @FXML private Button eszettCheckButton;
    @FXML private Button notPublishedButton;
    
    @FXML private Button startButton;
    @FXML private Button cancelButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Label runtimeLabel;
    
    // Results Section
    @FXML private TabPane resultsTabPane;
    @FXML private TextArea statisticsArea;
    @FXML private TextArea logArea;
    
    // Status Bar
    @FXML private Label statusBarLabel;
    @FXML private Label dbStatusLabel;
    @FXML private Label lastJobLabel;
    
    // State
    private Task<JobResult> currentTask;
    private String selectedJobType;
    private long jobStartTime;
    
    public MainController() {
        this.configService = new ConfigService();
        this.jobService = new JobService();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing MainController");
        
        // Initialize country code combo box
        countryCodeCombo.getItems().addAll("CH", "AT", "DE", "FR", "IT");
        
        // Set up tooltips
        setupTooltips();
        
        // Load last configuration
        configService.loadLastConfig();
        updateUIFromConfig();
        
        // Set up event handlers
        setupEventHandlers();
        
        // Initialize UI state
        updateJobButtonsState();
        updateStatusBar();
        
        logger.info("MainController initialized");
    }
    
    private void setupTooltips() {
        dbUrlField.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.url")));
        dbUsernameField.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.username")));
        dbPasswordField.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.password")));
        countryCodeCombo.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.country")));
        eszettCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.eszett")));
        regexCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.regex")));
        currentFileField.setTooltip(new Tooltip(I18nUtil.get("tooltip.paths.current")));
        previousFileField.setTooltip(new Tooltip(I18nUtil.get("tooltip.paths.previous")));
        outputDirField.setTooltip(new Tooltip(I18nUtil.get("tooltip.paths.output")));
    }
    
    private void setupEventHandlers() {
        // Database test button
        dbTestButton.setOnAction(e -> testDatabaseConnection());
        
        // File browsers
        currentFileBrowseButton.setOnAction(e -> browseFile(currentFileField, "Select Current File"));
        previousFileBrowseButton.setOnAction(e -> browseFile(previousFileField, "Select Previous File"));
        outputDirBrowseButton.setOnAction(e -> browseDirectory(outputDirField, "Select Output Directory"));
        
        // Config buttons
        saveConfigButton.setOnAction(e -> saveConfiguration());
        loadConfigButton.setOnAction(e -> loadConfiguration());
        restoreDefaultsButton.setOnAction(e -> restoreDefaults());
        
        // Job buttons
        overviewButton.setOnAction(e -> selectJob("overview"));
        descAddButton.setOnAction(e -> selectJob("desc-add"));
        descInactButton.setOnAction(e -> selectJob("desc-inact"));
        translateDeltaButton.setOnAction(e -> selectJob("translate-delta"));
        eszettCheckButton.setOnAction(e -> selectJob("eszett-check"));
        notPublishedButton.setOnAction(e -> selectJob("not-published"));
        
        // Start/Cancel buttons
        startButton.setOnAction(e -> startJob());
        cancelButton.setOnAction(e -> cancelJob());
        
        // Update config when fields change
        dbUrlField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        dbUsernameField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        dbPasswordField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        countryCodeCombo.valueProperty().addListener((obs, old, val) -> updateConfigFromUI());
        eszettCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
        regexCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
        currentFileField.textProperty().addListener((obs, old, val) -> {
            updateConfigFromUI();
            validateConfiguration();
        });
        previousFileField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        outputDirField.textProperty().addListener((obs, old, val) -> {
            updateConfigFromUI();
            validateConfiguration();
        });
    }
    
    private void updateUIFromConfig() {
        AppConfig config = configService.getCurrentConfig();
        
        dbUrlField.setText(config.getDatabase().getUrl());
        dbUsernameField.setText(config.getDatabase().getUsername());
        dbPasswordField.setText(config.getDatabase().getPassword());
        
        countryCodeCombo.setValue(config.getSettings().getCountryCode());
        eszettCheckBox.setSelected(config.getSettings().isTransformEszett());
        regexCheckBox.setSelected(config.getSettings().isRegexCheck());
        
        currentFileField.setText(config.getPaths().getCurrentFile());
        previousFileField.setText(config.getPaths().getPreviousFile());
        outputDirField.setText(config.getPaths().getOutputDirectory());
    }
    
    private void updateConfigFromUI() {
        AppConfig config = configService.getCurrentConfig();
        
        config.getDatabase().setUrl(dbUrlField.getText());
        config.getDatabase().setUsername(dbUsernameField.getText());
        config.getDatabase().setPassword(dbPasswordField.getText());
        
        if (countryCodeCombo.getValue() != null) {
            config.getSettings().setCountryCode(countryCodeCombo.getValue());
        }
        config.getSettings().setTransformEszett(eszettCheckBox.isSelected());
        config.getSettings().setRegexCheck(regexCheckBox.isSelected());
        
        config.getPaths().setCurrentFile(currentFileField.getText());
        config.getPaths().setPreviousFile(previousFileField.getText());
        config.getPaths().setOutputDirectory(outputDirField.getText());
    }
    
    private void testDatabaseConnection() {
        updateConfigFromUI();
        Conf conf = configService.toConf();
        
        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                DbConnection dbConn = new DbConnection(null, conf);
                try {
                    dbConn.connect();
                    dbConn.disconnect();
                    return true;
                } catch (Exception e) {
                    logger.error("Database connection test failed", e);
                    throw e;
                }
            }
        };
        
        testTask.setOnSucceeded(e -> {
            if (testTask.getValue()) {
                showInfo(I18nUtil.get("success.title"), 
                        I18nUtil.get("config.database.test.success"));
                updateStatusBar();
            } else {
                showError(I18nUtil.get("error.title"), 
                         I18nUtil.get("config.database.test.failure", "Connection is null"));
            }
        });
        
        testTask.setOnFailed(e -> {
            Throwable ex = testTask.getException();
            showError(I18nUtil.get("error.title"), 
                     I18nUtil.get("config.database.test.failure", ex.getMessage()));
        });
        
        new Thread(testTask).start();
    }
    
    private void browseFile(TextField targetField, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Supported Files", "*.csv", "*.tsv", "*.xlsx", "*.xls", "*.json"),
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("TSV Files", "*.tsv"),
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        // Set initial directory if field has a value
        String currentPath = targetField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileChooser.setInitialDirectory(currentFile.getParentFile());
            }
        }
        
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            targetField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void browseDirectory(TextField targetField, String title) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle(title);
        
        // Set initial directory if field has a value
        String currentPath = targetField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                dirChooser.setInitialDirectory(currentDir);
            }
        }
        
        File selectedDir = dirChooser.showDialog(stage);
        if (selectedDir != null) {
            targetField.setText(selectedDir.getAbsolutePath());
        }
    }
    
    private void saveConfiguration() {
        updateConfigFromUI();
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Configuration");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        fileChooser.setInitialFileName("levi-config.json");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                configService.saveConfig(file);
                showInfo(I18nUtil.get("success.title"), 
                        I18nUtil.get("success.config.saved"));
            } catch (Exception e) {
                logger.error("Error saving configuration", e);
                showError(I18nUtil.get("error.title"), 
                         I18nUtil.get("error.config.save") + ": " + e.getMessage());
            }
        }
    }
    
    private void loadConfiguration() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Configuration");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );
        
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                configService.loadConfig(file);
                updateUIFromConfig();
                showInfo(I18nUtil.get("success.title"), 
                        I18nUtil.get("success.config.loaded"));
            } catch (Exception e) {
                logger.error("Error loading configuration", e);
                showError(I18nUtil.get("error.title"), 
                         I18nUtil.get("error.config.load") + ": " + e.getMessage());
            }
        }
    }
    
    private void restoreDefaults() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Restore Defaults");
        alert.setHeaderText("Restore default configuration?");
        alert.setContentText("This will reset all settings to their default values.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                configService.restoreDefaults();
                updateUIFromConfig();
            }
        });
    }
    
    private void selectJob(String jobType) {
        this.selectedJobType = jobType;
        logger.info("Selected job: {}", jobType);
        updateJobButtonsState();
    }
    
    private void startJob() {
        if (selectedJobType == null) {
            showWarning("No Job Selected", "Please select a job to run.");
            return;
        }
        
        // Validate configuration
        updateConfigFromUI();
        String validationError = configService.validateConfig();
        if (validationError != null) {
            showError("Configuration Error", validationError);
            return;
        }
        
        // Create task based on job type
        Conf conf = configService.toConf();
        
        switch (selectedJobType) {
            case "overview":
                currentTask = jobService.createOverviewTask(conf);
                break;
            case "desc-add":
                currentTask = jobService.createDescAdditionsTask(conf);
                break;
            case "desc-inact":
                currentTask = jobService.createDescInactivationsTask(conf);
                break;
            case "translate-delta":
                currentTask = jobService.createTranslateDeltaTask(conf);
                break;
            case "eszett-check":
                currentTask = jobService.createEszettCheckTask(conf);
                break;
            case "not-published":
                currentTask = jobService.createNotPublishedTask(conf);
                break;
            default:
                showError("Unknown Job", "Unknown job type: " + selectedJobType);
                return;
        }
        
        // Set up task handlers
        setupTaskHandlers(currentTask);
        
        // Start task
        jobStartTime = System.currentTimeMillis();
        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
        
        // Update UI
        updateJobRunningState(true);
        startRuntimeUpdater();
    }
    
    private void setupTaskHandlers(Task<JobResult> task) {
        // Progress
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        
        // Success
        task.setOnSucceeded(e -> {
            JobResult result = task.getValue();
            displayResult(result);
            updateJobRunningState(false);
            updateLastJobStatus(result);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
        });
        
        // Failure
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Job failed", ex);
            logMessage("ERROR: " + ex.getMessage());
            showError("Job Failed", I18nUtil.get("error.job.failed", ex.getMessage()));
            updateJobRunningState(false);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
        });
        
        // Cancelled
        task.setOnCancelled(e -> {
            logMessage("Job cancelled by user");
            updateJobRunningState(false);
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
        });
    }
    
    private void cancelJob() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }
    
    private void displayResult(JobResult result) {
        StringBuilder stats = new StringBuilder();
        stats.append("=== ").append(I18nUtil.get("results.title")).append(": ")
             .append(result.getJobType()).append(" ===\n\n");
        
        if (result.isSuccessful()) {
            stats.append("✅ ").append(I18nUtil.get("jobs.status.success")).append("\n\n");
            
            stats.append(I18nUtil.get("results.statistics.additions")).append(" ")
                 .append(result.getAdditionsCount()).append("\n");
            stats.append(I18nUtil.get("results.statistics.changes")).append(" ")
                 .append(result.getChangesCount()).append("\n");
            stats.append(I18nUtil.get("results.statistics.inactivations")).append(" ")
                 .append(result.getInactivationsCount()).append("\n");
            stats.append(I18nUtil.get("results.statistics.reactivations")).append(" ")
                 .append(result.getReactivationsCount()).append("\n\n");
            
            stats.append(I18nUtil.get("results.statistics.errors")).append(" ")
                 .append(result.getErrorsCount()).append("\n");
            stats.append(I18nUtil.get("results.statistics.warnings")).append(" ")
                 .append(result.getWarningsCount()).append("\n\n");
            
            long seconds = result.getExecutionTimeMs() / 1000;
            stats.append(I18nUtil.get("jobs.progress.runtime", formatDuration(seconds))).append("\n");
        } else {
            stats.append("❌ ").append(I18nUtil.get("jobs.status.failed")).append("\n\n");
            stats.append("Error: ").append(result.getErrorMessage()).append("\n");
        }
        
        statisticsArea.setText(stats.toString());
        resultsTabPane.getSelectionModel().select(0); // Select statistics tab
    }
    
    private void updateJobButtonsState() {
        boolean jobSelected = selectedJobType != null;
        startButton.setDisable(!jobSelected || currentTask != null);
        
        // Highlight selected job button
        overviewButton.setStyle(selectedJobType != null && selectedJobType.equals("overview") ? "-fx-background-color: #4CAF50;" : "");
        descAddButton.setStyle(selectedJobType != null && selectedJobType.equals("desc-add") ? "-fx-background-color: #4CAF50;" : "");
        descInactButton.setStyle(selectedJobType != null && selectedJobType.equals("desc-inact") ? "-fx-background-color: #4CAF50;" : "");
        translateDeltaButton.setStyle(selectedJobType != null && selectedJobType.equals("translate-delta") ? "-fx-background-color: #4CAF50;" : "");
        eszettCheckButton.setStyle(selectedJobType != null && selectedJobType.equals("eszett-check") ? "-fx-background-color: #4CAF50;" : "");
        notPublishedButton.setStyle(selectedJobType != null && selectedJobType.equals("not-published") ? "-fx-background-color: #4CAF50;" : "");
    }
    
    private void updateJobRunningState(boolean running) {
        startButton.setDisable(running);
        cancelButton.setDisable(!running);
        
        if (!running) {
            progressBar.setProgress(0);
            statusLabel.setText(I18nUtil.get("jobs.status.idle"));
            runtimeLabel.setText("");
        }
    }
    
    private void startRuntimeUpdater() {
        Thread updater = new Thread(() -> {
            while (currentTask != null && !currentTask.isDone()) {
                long elapsed = System.currentTimeMillis() - jobStartTime;
                long seconds = elapsed / 1000;
                Platform.runLater(() -> {
                    runtimeLabel.setText(I18nUtil.get("jobs.progress.runtime", formatDuration(seconds)));
                });
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updater.setDaemon(true);
        updater.start();
    }
    
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    private void validateConfiguration() {
        String error = configService.validateConfig();
        
        // Update field styles based on validation
        currentFileField.setStyle(currentFileField.getText().isEmpty() ? "-fx-border-color: red;" : "");
        outputDirField.setStyle(outputDirField.getText().isEmpty() ? "-fx-border-color: red;" : "");
    }
    
    private void updateStatusBar() {
        statusBarLabel.setText(I18nUtil.get("status.idle"));
        
        // Test DB connection in background
        Task<Boolean> dbTest = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    Conf conf = configService.toConf();
                    DbConnection dbConn = new DbConnection(null, conf);
                    dbConn.connect();
                    dbConn.disconnect();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
        
        dbTest.setOnSucceeded(e -> {
            if (dbTest.getValue()) {
                dbStatusLabel.setText(I18nUtil.get("status.db.connected"));
                dbStatusLabel.setStyle("-fx-text-fill: green;");
            } else {
                dbStatusLabel.setText(I18nUtil.get("status.db.disconnected"));
                dbStatusLabel.setStyle("-fx-text-fill: red;");
            }
        });
        
        dbTest.setOnFailed(e -> {
            dbStatusLabel.setText(I18nUtil.get("status.db.disconnected"));
            dbStatusLabel.setStyle("-fx-text-fill: red;");
        });
        
        new Thread(dbTest).start();
    }
    
    private void updateLastJobStatus(JobResult result) {
        String status = result.isSuccessful() ? "✅ " + I18nUtil.get("jobs.status.success") 
                                              : "❌ " + I18nUtil.get("jobs.status.failed");
        long seconds = result.getExecutionTimeMs() / 1000;
        lastJobLabel.setText(I18nUtil.get("status.lastjob", 
                                         result.getJobType(), 
                                         formatDuration(seconds), 
                                         status));
    }
    
    private void logMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
        });
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    @FXML
    private void exitApplication() {
        Platform.exit();
    }
    
    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About LEVI for SNOMED");
        alert.setHeaderText("LEVI for SNOMED - Version 1.0.0");
        alert.setContentText("Language and Extension Validation & Import for SNOMED\n\n" +
                           "© 2025 eHealth Suisse\n\n" +
                           "This application provides a desktop GUI for managing SNOMED CT " +
                           "translation validation and delta generation.");
        alert.showAndWait();
    }
    
    @FXML
    private void clearLog() {
        logArea.clear();
    }
    
    @FXML
    private void saveLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Log");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Log Files", "*.log", "*.txt")
        );
        fileChooser.setInitialFileName("levi-log.txt");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                java.nio.file.Files.writeString(file.toPath(), logArea.getText());
                showInfo("Success", "Log saved successfully");
            } catch (Exception e) {
                logger.error("Error saving log", e);
                showError("Error", "Failed to save log: " + e.getMessage());
            }
        }
    }
}
