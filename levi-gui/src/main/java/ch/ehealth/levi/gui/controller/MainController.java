package ch.ehealth.levi.gui.controller;

import ch.ehealth.levi.gui.model.AppConfig;
import ch.ehealth.levi.gui.model.AppConfig.GitHubConfig;
import ch.ehealth.levi.gui.model.JobResult;
import ch.ehealth.levi.gui.service.ConfigService;
import ch.ehealth.levi.gui.service.GitHubUploadService;
import ch.ehealth.levi.gui.service.JobService;
import ch.ehealth.levi.gui.util.GuiInputStream;
import ch.ehealth.levi.gui.util.GuiLogAppender;
import ch.ehealth.levi.gui.util.I18nUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.ehealth.levi.core.Conf;
import ch.ehealth.levi.core.DbConnection;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main controller for the LEVI GUI application
 */
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    // Services
    private final ConfigService configService;
    private final JobService jobService;
    private final GitHubUploadService gitHubUploadService;
    
    // Stage
    private Stage stage;
    
    // Configuration Section
    @FXML private TextField dbNameField;
    @FXML private TextField dbPortField;
    @FXML private TextField dbUsernameField;
    @FXML private PasswordField dbPasswordField;
    @FXML private Button dbTestButton;
    
    @FXML private CheckBox eszettCheckBox;
    @FXML private CheckBox regexCheckBox;
    @FXML private CheckBox groupingCheckBox;
    
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

    // GitHub Upload
    @FXML private TextField githubRepoField;
    @FXML private TextField githubBranchField;
    @FXML private PasswordField githubTokenField;
    @FXML private CheckBox githubAutoUploadCheckBox;
    @FXML private Button uploadGitHubButton;
    
    // Results Section
    @FXML private TabPane resultsTabPane;
    @FXML private TextArea statisticsArea;
    @FXML private TextArea logArea;
    @FXML private HBox logInputBox;
    @FXML private TextField logInputField;

    // Main scroll pane (center of the BorderPane)
    @FXML private ScrollPane mainScrollPane;
    
    // Status Bar
    @FXML private Label statusBarLabel;
    @FXML private Label dbStatusLabel;
    @FXML private Label lastJobLabel;
    
    // State
    private Task<JobResult> currentTask;
    private final List<String> selectedJobTypes = new ArrayList<>();
    private long jobStartTime;
    private GuiInputStream guiInputStream;
    private static final InputStream ORIGINAL_STDIN = System.in;
    
    public MainController() {
        this.configService = new ConfigService();
        this.jobService = new JobService();
        this.gitHubUploadService = new GitHubUploadService();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing MainController");
        
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

        // Wire GUI log appender so LEVI core logs appear in the log area
        GuiLogAppender.setLogArea(logArea);
        // Enter key in the input field acts the same as clicking Submit
        logInputField.setOnAction(e -> submitLogInput());

        logger.info("MainController initialized");
    }
    
    private void setupTooltips() {
        dbNameField.setTooltip(new Tooltip("Database name, e.g. SCT:CH_Dec25"));
        dbPortField.setTooltip(new Tooltip("MySQL port, default 3306"));
        dbUsernameField.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.username")));
        dbPasswordField.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.password")));
        eszettCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.eszett")));
        regexCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.regex")));
        groupingCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.grouping")));
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
        
        // GitHub upload button
        uploadGitHubButton.setOnAction(e -> uploadToGitHub());

        // Update config when fields change
        dbNameField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        dbPortField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        dbUsernameField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        dbPasswordField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        eszettCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
        regexCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
        groupingCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
        currentFileField.textProperty().addListener((obs, old, val) -> {
            updateConfigFromUI();
            validateConfiguration();
        });
        previousFileField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        outputDirField.textProperty().addListener((obs, old, val) -> {
            updateConfigFromUI();
            validateConfiguration();
        });

        // GitHub field listeners
        githubRepoField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        githubBranchField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        githubTokenField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        githubAutoUploadCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
    }
    
    private void updateUIFromConfig() {
        AppConfig config = configService.getCurrentConfig();
        
        dbNameField.setText(config.getDatabase().getDbName());
        dbPortField.setText(String.valueOf(config.getDatabase().getDbPort()));
        dbUsernameField.setText(config.getDatabase().getUsername());
        dbPasswordField.setText(config.getDatabase().getPassword());
        
        eszettCheckBox.setSelected(config.getSettings().isTransformEszett());
        regexCheckBox.setSelected(config.getSettings().isRegexCheck());
        groupingCheckBox.setSelected(config.getSettings().isGrouping());
        currentFileField.setText(config.getPaths().getCurrentFile());
        previousFileField.setText(config.getPaths().getPreviousFile());
        outputDirField.setText(config.getPaths().getOutputDirectory());

        if (config.getGithub() != null) {
            githubRepoField.setText(config.getGithub().getRepoUrl());
            githubBranchField.setText(config.getGithub().getBranch());
            githubTokenField.setText(config.getGithub().getToken());
            githubAutoUploadCheckBox.setSelected(config.getGithub().isAutoUpload());
        }
    }
    
    private void updateConfigFromUI() {
        AppConfig config = configService.getCurrentConfig();
        
        config.getDatabase().setDbName(dbNameField.getText());
        try {
            config.getDatabase().setDbPort(Integer.parseInt(dbPortField.getText().trim()));
        } catch (NumberFormatException ignored) {
            // keep previous port if input is not a valid number
        }
        config.getDatabase().setUsername(dbUsernameField.getText());
        config.getDatabase().setPassword(dbPasswordField.getText());
        
        config.getSettings().setTransformEszett(eszettCheckBox.isSelected());
        config.getSettings().setRegexCheck(regexCheckBox.isSelected());
        config.getSettings().setGrouping(groupingCheckBox.isSelected());
        
        config.getPaths().setCurrentFile(currentFileField.getText());
        config.getPaths().setPreviousFile(previousFileField.getText());
        config.getPaths().setOutputDirectory(outputDirField.getText());

        if (config.getGithub() != null) {
            config.getGithub().setRepoUrl(githubRepoField.getText());
            config.getGithub().setBranch(githubBranchField.getText());
            config.getGithub().setToken(githubTokenField.getText());
            config.getGithub().setAutoUpload(githubAutoUploadCheckBox.isSelected());
        }
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
    
    @FXML
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
    
    @FXML
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
    
    @FXML
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
        if (selectedJobTypes.contains(jobType)) {
            selectedJobTypes.remove(jobType);
        } else {
            selectedJobTypes.add(jobType);
        }
        logger.info("Job queue: {}", selectedJobTypes);
        updateJobButtonsState();
    }
    
    private void startJob() {
        if (selectedJobTypes.isEmpty()) {
            showWarning("No Job Selected", "Please select at least one job to run.");
            return;
        }
        
        // Validate configuration
        updateConfigFromUI();
        String validationError = configService.validateConfig();
        if (validationError != null) {
            showError("Configuration Error", validationError);
            return;
        }
        
        File outputDir = new File(configService.getCurrentConfig().getPaths().getOutputDirectory());
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                showError("Configuration Error", "Cannot create output directory: " + outputDir.getAbsolutePath());
                return;
            }
        }
        
        List<String> queue = new ArrayList<>(selectedJobTypes);
        updateJobRunningState(true);
        startRuntimeUpdater();
        runNextJob(queue, 0, null);
    }

    private void runNextJob(List<String> queue, int index, JobResult prevResult) {
        String jobType = queue.get(index);
        Conf conf = configService.toConf();

        Task<JobResult> task;
        switch (jobType) {
            case "overview":        task = jobService.createOverviewTask(conf);          break;
            case "desc-add":        task = jobService.createDescAdditionsTask(conf);     break;
            case "desc-inact":      task = jobService.createDescInactivationsTask(conf); break;
            case "translate-delta": task = jobService.createTranslateDeltaTask(conf);   break;
            case "eszett-check":    task = jobService.createEszettCheckTask(conf);       break;
            case "not-published": {
                // Reuse the manager from the previous job if it already loaded the current file
            	ch.ehealth.levi.core.compare.CompareManager preloaded =
                        (prevResult != null) ? prevResult.getManager() : null;
                task = jobService.createNotPublishedTask(conf, preloaded);
                break;
            }
            default:
                showError("Unknown Job", "Unknown job type: " + jobType);
                updateJobRunningState(false);
                updateJobButtonsState();
                return;
        }

        currentTask = task;

        // Switch to Progress tab and show starting message
        statisticsArea.clear();
        String ts = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (queue.size() > 1) {
            appendProgress("[" + ts + "] Starting job " + (index + 1) + "/" + queue.size() + ": " + jobType);
        } else {
            appendProgress("[" + ts + "] Starting: " + jobType);
        }
        resultsTabPane.getSelectionModel().select(0);
        Platform.runLater(() -> mainScrollPane.setVvalue(1.0));

        // Redirect System.in so stdin prompts from LEVI core are handled by the GUI
        guiInputStream = new GuiInputStream(this::showLogInputPrompt);
        System.setIn(guiInputStream);

        jobStartTime = System.currentTimeMillis();
        setupTaskHandlers(task, queue, index);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void setupTaskHandlers(Task<JobResult> task, List<String> queue, int index) {
        // Pipe task status messages to Progress tab in real time
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && !newMsg.isEmpty()) {
                appendProgress(newMsg);
            }
        });

        // Progress bar — indeterminate (no fake percentages)
        progressBar.setProgress(-1);
        statusLabel.textProperty().bind(task.messageProperty());

        // Success
        task.setOnSucceeded(e -> {
            JobResult result = task.getValue();
            currentTask = null;
            cleanupInputStream();
            String doneTs = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            appendProgress("[" + doneTs + "] " + (result.isSuccessful() ? "Completed" : "Failed")
                    + " (" + formatDuration(result.getExecutionTimeMs() / 1000) + ")");
            displayResult(result);
            progressBar.setProgress(0);
            statusLabel.textProperty().unbind();
            updateLastJobStatus(result);
            if (index + 1 < queue.size()) {
                // Chain to next job in queue
                startRuntimeUpdater();
                Platform.runLater(() -> runNextJob(queue, index + 1, result));
            } else {
                updateJobRunningState(false);
                updateJobButtonsState();
                // Auto-upload to GitHub if enabled
                if (result.isSuccessful() && configService.getCurrentConfig().getGithub() != null
                        && configService.getCurrentConfig().getGithub().isAutoUpload()) {
                    updateConfigFromUI();
                    Platform.runLater(() -> uploadToGitHub());
                }
            }
        });

        // Failure
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Job failed", ex);
            currentTask = null;
            cleanupInputStream();
            appendProgress("ERROR: " + (ex != null ? ex.getMessage() : "unknown error"));
            progressBar.setProgress(0);
            statusLabel.textProperty().unbind();
            updateJobRunningState(false);
            updateJobButtonsState();
            showError("Job Failed", "Job failed: " + (ex != null ? ex.getMessage() : "unknown error"));
        });

        // Cancelled
        task.setOnCancelled(e -> {
            currentTask = null;
            cleanupInputStream();
            appendProgress("Job cancelled");
            progressBar.setProgress(0);
            statusLabel.textProperty().unbind();
            updateJobRunningState(false);
            updateJobButtonsState();
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
        stats.append("=== ").append(result.getJobType()).append(" ===\n\n");
        
        if (result.isSuccessful()) {
            stats.append("Successful\n\n");
            
            stats.append("Additions:    ").append(result.getAdditionsCount()).append("\n");
            stats.append("Changes:      ").append(result.getChangesCount()).append("\n");
            stats.append("Inactivations: ").append(result.getInactivationsCount()).append("\n");
            stats.append("Reactivations: ").append(result.getReactivationsCount()).append("\n\n");
            
            stats.append("Errors:   ").append(result.getErrorsCount()).append("\n");
            stats.append("Warnings: ").append(result.getWarningsCount()).append("\n\n");
            
            long seconds = result.getExecutionTimeMs() / 1000;
            stats.append("Runtime: ").append(formatDuration(seconds)).append("\n");
        } else {
            stats.append("Failed\n\n");
            stats.append("Error: ").append(result.getErrorMessage()).append("\n");
        }
        
        statisticsArea.appendText(stats.toString());
        resultsTabPane.getSelectionModel().select(0);
    }
    
    private void updateJobButtonsState() {
        startButton.setDisable(selectedJobTypes.isEmpty() || currentTask != null);

        updateJobButton(overviewButton,       "Translation Overview",    "overview");
        updateJobButton(descAddButton,        "New Descriptions",        "desc-add");
        updateJobButton(descInactButton,      "Inactivations",           "desc-inact");
        updateJobButton(translateDeltaButton, "Complete Delta",          "translate-delta");
        updateJobButton(eszettCheckButton,    "Eszett Check",            "eszett-check");
        updateJobButton(notPublishedButton,   "Unpublished Translations", "not-published");
    }

    private void updateJobButton(Button btn, String baseLabel, String jobType) {
        int idx = selectedJobTypes.indexOf(jobType);
        if (idx >= 0) {
            btn.setText(baseLabel + " [" + (idx + 1) + "]");
            btn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        } else {
            btn.setText(baseLabel);
            btn.setStyle("");
        }
    }
    
    private void updateJobRunningState(boolean running) {
        startButton.setDisable(running);
        cancelButton.setDisable(!running);
        uploadGitHubButton.setDisable(running);
        
        if (running) {
            progressBar.setProgress(-1);
        } else {
            progressBar.setProgress(0);
            statusLabel.setText("Idle");
            runtimeLabel.setText("");
        }
    }
    
    private void startRuntimeUpdater() {
        Thread updater = new Thread(() -> {
            while (currentTask != null && !currentTask.isDone()) {
                long elapsed = System.currentTimeMillis() - jobStartTime;
                long seconds = elapsed / 1000;
                Platform.runLater(() -> {
                    runtimeLabel.setText("Runtime: " + formatDuration(seconds));
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
        statusBarLabel.setText("Idle");
        
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
        String status = result.isSuccessful() ? "Successful" : "Failed";
        long seconds = result.getExecutionTimeMs() / 1000;
        lastJobLabel.setText("Last Job: " + result.getJobType() + ", "
                + formatDuration(seconds) + ", " + status);
    }
    
    @FXML
    private void uploadToGitHub() {
        updateConfigFromUI();
        GitHubConfig gitHubConfig = configService.getCurrentConfig().getGithub();

        if (gitHubConfig == null || gitHubConfig.getRepoUrl() == null || gitHubConfig.getRepoUrl().isEmpty()) {
            showWarning("GitHub Upload", "No repository configured. Set your repo in Configuration → GitHub Upload.");
            return;
        }

        String outputDir = configService.getCurrentConfig().getPaths().getOutputDirectory();
        if (outputDir == null || outputDir.isEmpty()) {
            showWarning("GitHub Upload", "No output directory configured.");
            return;
        }

        if (gitHubUploadService.isUploading()) {
            logMessage("Upload already in progress...");
            return;
        }

        Task<String> uploadTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Uploading results to GitHub...");
                return gitHubUploadService.uploadResults(outputDir, gitHubConfig);
            }
        };

        uploadTask.setOnSucceeded(e -> {
            String result = uploadTask.getValue();
            logMessage("✅ " + result);
            statusLabel.setText("Upload complete");
            uploadGitHubButton.setDisable(false);
        });

        uploadTask.setOnFailed(e -> {
            Throwable ex = uploadTask.getException();
            String msg = (ex != null) ? ex.getMessage() : "Unknown error";
            logMessage("❌ GitHub upload failed: " + msg);
            statusLabel.setText("Upload failed");
            uploadGitHubButton.setDisable(false);
        });

        uploadGitHubButton.setDisable(true);
        logMessage("Starting GitHub upload...");
        statusLabel.setText("Uploading to GitHub...");

        Thread thread = new Thread(uploadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void showLogInputPrompt() {
        if (logInputBox == null) return; // guard against FXML injection failure
        logInputBox.setVisible(true);
        logInputBox.setManaged(true);
        logInputField.clear();
        resultsTabPane.getSelectionModel().select(1); // ensure Log tab is visible
        mainScrollPane.setVvalue(1.0);              // scroll so the input bar is on screen
        logInputField.requestFocus();
    }

    private void hideLogInputPrompt() {
        logInputBox.setVisible(false);
        logInputBox.setManaged(false);
    }

    private void cleanupInputStream() {
        hideLogInputPrompt();
        System.setIn(ORIGINAL_STDIN);
        if (guiInputStream != null) {
            guiInputStream.close();
            guiInputStream = null;
        }
    }

    @FXML
    private void submitLogInput() {
        String input = logInputField.getText();
        logMessage("  > " + input);  // echo to log area
        hideLogInputPrompt();
        if (guiInputStream != null) {
            guiInputStream.provideInput(input);
        }
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
        });
    }

    private void appendProgress(String message) {
        Platform.runLater(() -> {
            statisticsArea.appendText(message + "\n");
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
        int year = java.time.LocalDate.now().getYear();

        Hyperlink link = new Hyperlink("https://github.com/ehealthsuisse/LEVI-for-SNOMED/");
        link.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                    new java.net.URI("https://github.com/ehealthsuisse/LEVI-for-SNOMED/")
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(8);
        content.getChildren().addAll(
            new javafx.scene.control.Label(
                "Language and Extension Validation & Import for SNOMED\n\n" +
                "© " + year + " eHealth Suisse\n\n" +
                "This application provides a desktop GUI for managing SNOMED CT " +
                "translation validation and delta generation.\n"
            ),
            link
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About LEVI for SNOMED");
        alert.setHeaderText("LEVI for SNOMED - Version 1.0.0");
        alert.getDialogPane().setContent(content);
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
