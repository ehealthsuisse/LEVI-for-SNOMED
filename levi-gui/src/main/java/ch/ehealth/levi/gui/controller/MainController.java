package ch.ehealth.levi.gui.controller;

import ch.ehealth.levi.gui.model.AppConfig;
import ch.ehealth.levi.gui.model.AppConfig.GitHubConfig;
import ch.ehealth.levi.gui.model.JobResult;
import ch.ehealth.levi.gui.service.ConfigService;
import ch.ehealth.levi.gui.service.GitHubUploadService;
import ch.ehealth.levi.gui.service.JobService;
import ch.ehealth.levi.gui.util.GuiLogAppender;
import ch.ehealth.levi.gui.util.I18nUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    @FXML private ComboBox<String> dbListComboBox;
    @FXML private TextField dbPortField;
    @FXML private TextField dbUsernameField;
    @FXML private PasswordField dbPasswordField;
    @FXML private Button dbListButton;
    @FXML private Button dbTestButton;
    
    @FXML private ComboBox<String> countryCodeComboBox;
    @FXML private ComboBox<String> languageFilterComboBox;
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

    // Main scroll pane (center of the BorderPane)
    @FXML private ScrollPane mainScrollPane;
    
    // Status Bar
    @FXML private Label statusBarLabel;
    @FXML private Label dbStatusLabel;
    @FXML private Label lastJobLabel;
    
    // Language menu
    @FXML private RadioMenuItem languageEnMenuItem;
    @FXML private RadioMenuItem languageDeMenuItem;
    @FXML private RadioMenuItem languageFrMenuItem;
    @FXML private RadioMenuItem languageItMenuItem;
    @FXML private ToggleGroup languageToggleGroup;
    
    // State
    private Task<JobResult> currentTask;
    private final List<String> selectedJobTypes = new ArrayList<>();
    private long jobStartTime;
    private boolean preflightOk = false;
    private final PauseTransition dbNameDebounce = new PauseTransition(Duration.millis(800));
    private Task<Boolean> preflightTask;
    
    public MainController() {
        this.configService = new ConfigService();
        this.jobService = new JobService();
        this.gitHubUploadService = new GitHubUploadService();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing MainController");
        
        setupTooltips();
        
        countryCodeComboBox.getItems().setAll(ch.ehealth.levi.core.Conf.getAvailableCountryCodes());
        countryCodeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (val != null) {
                        updateLanguageFilterItems(val);
                    }
                    runPreflightCheck();
                });

        languageFilterComboBox.getItems().setAll(I18nUtil.get("language.filter.all"), "de", "fr", "it");

        configService.loadLastConfig();
        updateUIFromConfig();
        loadAvailableDatabases();
        
        setupEventHandlers();

        updateJobButtonsState();
        runPreflightCheck();

        GuiLogAppender.setLogArea(logArea);

        selectLanguageMenuItem();

        logger.info("MainController initialized");
    }

    private void selectLanguageMenuItem() {
        String lang = I18nUtil.getCurrentLocale().getLanguage();
        switch (lang) {
            case "de": languageDeMenuItem.setSelected(true); break;
            case "fr": languageFrMenuItem.setSelected(true); break;
            case "it": languageItMenuItem.setSelected(true); break;
            default:   languageEnMenuItem.setSelected(true); break;
        }
    }
    
    @FXML
    private void switchToEnglish() {
        switchLanguage("en");
    }
    
    @FXML
    private void switchToGerman() {
        switchLanguage("de");
    }
    
    @FXML
    private void switchToFrench() {
        switchLanguage("fr");
    }
    
    @FXML
    private void switchToItalian() {
        switchLanguage("it");
    }
    
    private void switchLanguage(String languageCode) {
        I18nUtil.setLocaleByLanguageCode(languageCode);
        I18nUtil.saveLanguagePreference(languageCode);
        reloadScene();
    }
    
    private void reloadScene() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/MainView.fxml"));
            loader.setResources(I18nUtil.getResourceBundle());
            MainController controller = new MainController();
            loader.setController(controller);
            Parent root = loader.load();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setTitle(I18nUtil.get("app.title"));
            stage.setScene(scene);
            controller.setStage(stage);
        } catch (IOException e) {
            logger.error("Failed to reload scene after language change", e);
        }
    }
    
    private void updateLanguageFilterItems(String countryCode) {
        Set<String> validLanguages = ch.ehealth.levi.core.Conf.getLanguagesForCountry(countryCode);
        String currentValue = languageFilterComboBox.getValue();
        List<String> items = new ArrayList<>();
        if (validLanguages.size() > 1) {
            items.add(I18nUtil.get("language.filter.all"));
        }
        validLanguages.stream().sorted().forEach(items::add);
        languageFilterComboBox.getItems().setAll(items);
        if (validLanguages.size() == 1) {
            languageFilterComboBox.setValue(validLanguages.iterator().next());
        } else if (currentValue != null && items.contains(currentValue)) {
            languageFilterComboBox.setValue(currentValue);
        } else {
            languageFilterComboBox.setValue(I18nUtil.get("language.filter.all"));
        }
    }

    private void setupTooltips() {
        dbListComboBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.name")));
        dbListButton.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.list")));
        dbPortField.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.port")));
        dbUsernameField.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.username")));
        dbPasswordField.setTooltip(new Tooltip(I18nUtil.get("tooltip.database.password")));
        countryCodeComboBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.country")));
        languageFilterComboBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.language.filter")));
        eszettCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.eszett")));
        regexCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.regex")));
        groupingCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.settings.grouping")));
        currentFileField.setTooltip(new Tooltip(I18nUtil.get("tooltip.paths.current")));
        previousFileField.setTooltip(new Tooltip(I18nUtil.get("tooltip.paths.previous")));
        outputDirField.setTooltip(new Tooltip(I18nUtil.get("tooltip.paths.output")));
        githubRepoField.setTooltip(new Tooltip(I18nUtil.get("tooltip.github.repo")));
        githubBranchField.setTooltip(new Tooltip(I18nUtil.get("tooltip.github.branch")));
        githubTokenField.setTooltip(new Tooltip(I18nUtil.get("tooltip.github.token")));
        githubAutoUploadCheckBox.setTooltip(new Tooltip(I18nUtil.get("tooltip.github.auto")));
    }
    
    private void setupEventHandlers() {
        dbTestButton.setOnAction(e -> testDatabaseConnection());
        
        currentFileBrowseButton.setOnAction(e -> browseFile(currentFileField, I18nUtil.get("filechooser.current")));
        previousFileBrowseButton.setOnAction(e -> browseFile(previousFileField, I18nUtil.get("filechooser.previous")));
        outputDirBrowseButton.setOnAction(e -> browseDirectory(outputDirField, I18nUtil.get("filechooser.output")));
        
        saveConfigButton.setOnAction(e -> saveConfiguration());
        loadConfigButton.setOnAction(e -> loadConfiguration());
        restoreDefaultsButton.setOnAction(e -> restoreDefaults());
        
        overviewButton.setOnAction(e -> selectJob("overview"));
        descAddButton.setOnAction(e -> selectJob("desc-add"));
        descInactButton.setOnAction(e -> selectJob("desc-inact"));
        translateDeltaButton.setOnAction(e -> selectJob("translate-delta"));
        eszettCheckButton.setOnAction(e -> selectJob("eszett-check"));
        notPublishedButton.setOnAction(e -> selectJob("not-published"));
        
        startButton.setOnAction(e -> startJob());
        cancelButton.setOnAction(e -> cancelJob());
        
        uploadGitHubButton.setOnAction(e -> uploadToGitHub());

        dbListButton.setOnAction(e -> loadAvailableDatabases(false));
        dbListComboBox.getEditor().textProperty().addListener((obs, old, val) -> {
            updateConfigFromUI();
            dbNameDebounce.setOnFinished(e -> runPreflightCheck());
            dbNameDebounce.playFromStart();
        });
        dbListComboBox.getEditor().focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                dbNameDebounce.stop();
                runPreflightCheck();
            }
        });
        dbPortField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        dbUsernameField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        dbPasswordField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        eszettCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
        regexCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
        groupingCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
        languageFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> updateConfigFromUI());
        currentFileField.textProperty().addListener((obs, old, val) -> {
            updateConfigFromUI();
            validateConfiguration();
        });
        previousFileField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        outputDirField.textProperty().addListener((obs, old, val) -> {
            updateConfigFromUI();
            validateConfiguration();
        });

        githubRepoField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        githubBranchField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        githubTokenField.textProperty().addListener((obs, old, val) -> updateConfigFromUI());
        githubAutoUploadCheckBox.selectedProperty().addListener((obs, old, val) -> updateConfigFromUI());
    }
    
    private boolean suppressConfigUpdates = false;
    
    private void updateUIFromConfig() {
        suppressConfigUpdates = true;
        try {
            AppConfig config = configService.getCurrentConfig();

            dbListComboBox.setValue(config.getDatabase().getDbName());
            dbPortField.setText(String.valueOf(config.getDatabase().getDbPort()));
            dbUsernameField.setText(config.getDatabase().getUsername());
            dbPasswordField.setText(config.getDatabase().getPassword());

            countryCodeComboBox.setValue(config.getSettings().getCountryCode());
            updateLanguageFilterItems(config.getSettings().getCountryCode());
            languageFilterComboBox.setValue(config.getSettings().getLanguageCodeFilter());
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
        } finally {
            suppressConfigUpdates = false;
        }
        
        validateConfiguration();
        runPreflightCheck();
    }
    
    private void updateConfigFromUI() {
    	if (suppressConfigUpdates) {
            return;
        }
    	
    	AppConfig config = configService.getCurrentConfig();
        
        config.getDatabase().setDbName(dbListComboBox.getEditor().getText());
        try {
            config.getDatabase().setDbPort(Integer.parseInt(dbPortField.getText().trim()));
        } catch (NumberFormatException ignored) {
        }
        config.getDatabase().setUsername(dbUsernameField.getText());
        config.getDatabase().setPassword(dbPasswordField.getText());
        
        config.getSettings().setCountryCode(countryCodeComboBox.getValue());
        config.getSettings().setLanguageCodeFilter(languageFilterComboBox.getValue());
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
                runPreflightCheck();
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

    private void loadAvailableDatabases() {
        loadAvailableDatabases(true);
    }

    private void loadAvailableDatabases(boolean silentOnFailure) {
        updateConfigFromUI();
        Conf conf = configService.toConf();

        Task<List<String>> listTask = new Task<List<String>>() {
            @Override
            protected List<String> call() throws Exception {
                return DbConnection.listDatabases(conf);
            }
        };

        listTask.setOnSucceeded(e -> {
            List<String> databases = listTask.getValue();
            dbListComboBox.getItems().setAll(databases);
            String currentDb = configService.getCurrentConfig().getDatabase().getDbName();
            if (currentDb != null && databases.contains(currentDb)) {
                dbListComboBox.setValue(currentDb);
            } else if (!databases.isEmpty()) {
                dbListComboBox.setValue(databases.get(0));
            }
        });

        listTask.setOnFailed(e -> {
            if (!silentOnFailure) {
                Throwable ex = listTask.getException();
                logger.error("Failed to list databases", ex);
                showError(I18nUtil.get("error.title"),
                        I18nUtil.get("config.database.list.failure", ex.getMessage()));
            }
        });

        new Thread(listTask).start();
    }

    private void runPreflightCheck() {
        if (preflightTask != null && preflightTask.isRunning()) {
            return;
        }

        String dbName = configService.getCurrentConfig().getDatabase().getDbName();
        if (dbName == null || dbName.isEmpty()) {
            preflightOk = false;
            Platform.runLater(() -> {
                dbStatusLabel.setText(I18nUtil.get("status.db.disconnected"));
                dbStatusLabel.setStyle("-fx-text-fill: red;");
                updateJobButtonsState();
            });
            return;
        }

        Conf conf = configService.toConf();
        Platform.runLater(() -> {
            dbStatusLabel.setText(I18nUtil.get("status.db.checking"));
            dbStatusLabel.setStyle("-fx-text-fill: gray;");
        });

        preflightTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                DbConnection dbConn = new DbConnection(null, conf);
                try {
                    dbConn.connect();
                    dbConn.validateRefsetIds();
                    return true;
                } finally {
                    try { dbConn.disconnect(); } catch (Exception ignored) {}
                }
            }
        };

        preflightTask.setOnSucceeded(e -> {
            preflightOk = true;
            dbStatusLabel.setText(I18nUtil.get("status.db.preflight.ok"));
            dbStatusLabel.setStyle("-fx-text-fill: green;");
            updateJobButtonsState();
        });

        preflightTask.setOnFailed(e -> {
            preflightOk = false;
            Throwable ex = preflightTask.getException();
            String msg = (ex != null) ? ex.getMessage() : "Unknown error";
            logger.warn("Preflight check failed: {}", msg);
            if (ex instanceof IllegalArgumentException) {
                dbStatusLabel.setText(I18nUtil.get("status.db.preflight.refset.failed"));
            } else {
                dbStatusLabel.setText(I18nUtil.get("status.db.preflight.connection.failed"));
            }
            dbStatusLabel.setStyle("-fx-text-fill: red;");
            dbStatusLabel.setTooltip(new Tooltip(msg));
            updateJobButtonsState();
        });

        new Thread(preflightTask).start();
    }
    
    private void browseFile(TextField targetField, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.all.supported"), "*.csv", "*.tsv", "*.xlsx", "*.xls", "*.json"),
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.csv"), "*.csv"),
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.tsv"), "*.tsv"),
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.excel"), "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.json"), "*.json"),
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.all.files"), "*.*")
        );
        
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
        fileChooser.setTitle(I18nUtil.get("filechooser.save.config"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.json"), "*.json")
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
        fileChooser.setTitle(I18nUtil.get("filechooser.load.config"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.json"), "*.json")
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
        alert.setTitle(I18nUtil.get("dialog.restore.title"));
        alert.setHeaderText(I18nUtil.get("dialog.restore.header"));
        alert.setContentText(I18nUtil.get("dialog.restore.content"));
        
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
            showWarning(I18nUtil.get("dialog.job.none.title"), 
                       I18nUtil.get("dialog.job.none.content"));
            return;
        }
        
        updateConfigFromUI();
        String validationError = configService.validateConfig();
        if (validationError != null) {
            showError(I18nUtil.get("dialog.config.error.title"), validationError);
            return;
        }
        
        File outputDir = new File(configService.getCurrentConfig().getPaths().getOutputDirectory());
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                showError(I18nUtil.get("dialog.config.error.title"), 
                         I18nUtil.get("dialog.config.error.output", outputDir.getAbsolutePath()));
                return;
            }
        }
        
        List<String> queue = new ArrayList<>(selectedJobTypes);
        // Not-published must run first: it compares the current file against the
        // previous file, so it needs a pristine collector before any other job
        // (e.g. translate-delta) processes the current data.
        queue.remove("not-published");
        queue.add(0, "not-published");

        statisticsArea.clear();
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
            case "translate-delta": {
                ch.ehealth.levi.core.compare.CompareManager preloaded =
                        (prevResult != null) ? prevResult.getManager() : null;
                task = jobService.createTranslateDeltaTask(conf, preloaded);
                break;
            }
            case "eszett-check":    task = jobService.createEszettCheckTask(conf);       break;
            case "not-published": {
            	ch.ehealth.levi.core.compare.CompareManager preloaded =
                        (prevResult != null) ? prevResult.getManager() : null;
                task = jobService.createNotPublishedTask(conf, preloaded);
                break;
            }
            default:
                showError(I18nUtil.get("dialog.job.unknown.title"), 
                         I18nUtil.get("dialog.job.unknown.content", jobType));
                updateJobRunningState(false);
                updateJobButtonsState();
                return;
        }

        currentTask = task;

        String ts = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (queue.size() > 1) {
            appendProgress("[" + ts + "] " + 
                I18nUtil.get("jobs.progress.starting.queue", index + 1, queue.size(), getJobDisplayName(jobType)));
        } else {
            appendProgress("[" + ts + "] " + 
                I18nUtil.get("jobs.progress.starting", getJobDisplayName(jobType)));
        }
        resultsTabPane.getSelectionModel().select(0);
        Platform.runLater(() -> mainScrollPane.setVvalue(1.0));

        jobStartTime = System.currentTimeMillis();
        setupTaskHandlers(task, queue, index);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void setupTaskHandlers(Task<JobResult> task, List<String> queue, int index) {
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (newMsg != null && !newMsg.isEmpty()) {
                appendProgress(newMsg);
            }
        });

        progressBar.setProgress(-1);
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            JobResult result = task.getValue();
            currentTask = null;
            String doneTs = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String statusText = result.isSuccessful() ? 
                I18nUtil.get("jobs.progress.completed") : I18nUtil.get("jobs.progress.failed.status");
            appendProgress("[" + doneTs + "] " + statusText 
                + " (" + formatDuration(result.getExecutionTimeMs() / 1000) + ")");
            displayResult(result);
            progressBar.setProgress(0);
            statusLabel.textProperty().unbind();
            updateLastJobStatus(result);
            if (index + 1 < queue.size()) {
                startRuntimeUpdater();
                Platform.runLater(() -> runNextJob(queue, index + 1, result));
            } else {
                updateJobRunningState(false);
                updateJobButtonsState();
                if (result.isSuccessful() && configService.getCurrentConfig().getGithub() != null
                        && configService.getCurrentConfig().getGithub().isAutoUpload()) {
                    updateConfigFromUI();
                    Platform.runLater(() -> uploadToGitHub());
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Job failed", ex);
            currentTask = null;
            appendProgress(I18nUtil.get("jobs.progress.error", ex != null ? ex.getMessage() : "unknown error"));
            progressBar.setProgress(0);
            statusLabel.textProperty().unbind();
            updateJobRunningState(false);
            updateJobButtonsState();
            showError(I18nUtil.get("dialog.job.failed.title"), 
                     I18nUtil.get("dialog.job.failed.content", ex != null ? ex.getMessage() : "unknown error"));
        });

        task.setOnCancelled(e -> {
            currentTask = null;
            appendProgress(I18nUtil.get("jobs.progress.cancelled"));
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
        stats.append(I18nUtil.get("results.statistics.header", result.getJobType())).append("\n\n");
        
        if (result.isSuccessful()) {
            stats.append(I18nUtil.get("results.statistics.success")).append("\n\n");
            
            stats.append(I18nUtil.get("results.statistics.additions")).append("   ").append(result.getAdditionsCount()).append("\n");
            stats.append(I18nUtil.get("results.statistics.changes")).append("     ").append(result.getChangesCount()).append("\n");
            stats.append(I18nUtil.get("results.statistics.inactivations")).append(" ").append(result.getInactivationsCount()).append("\n");
            stats.append(I18nUtil.get("results.statistics.reactivations")).append(" ").append(result.getReactivationsCount()).append("\n\n");
            
            stats.append(I18nUtil.get("results.statistics.errors")).append("  ").append(result.getErrorsCount()).append("\n");
            stats.append(I18nUtil.get("results.statistics.warnings")).append(" ").append(result.getWarningsCount()).append("\n\n");
            
            long seconds = result.getExecutionTimeMs() / 1000;
            stats.append(I18nUtil.get("results.display.runtime", formatDuration(seconds))).append("\n");
        } else {
            stats.append(I18nUtil.get("results.statistics.failure")).append("\n\n");
            stats.append(I18nUtil.get("results.statistics.error.label", result.getErrorMessage())).append("\n");
        }
        
        statisticsArea.appendText(stats.toString());
        resultsTabPane.getSelectionModel().select(0);
    }
    
    private void updateJobButtonsState() {
        startButton.setDisable(selectedJobTypes.isEmpty() || currentTask != null || !preflightOk);

        updateJobButton(overviewButton,       I18nUtil.get("jobs.overview"),       "overview");
        updateJobButton(descAddButton,        I18nUtil.get("jobs.desc_add"),        "desc-add");
        updateJobButton(descInactButton,      I18nUtil.get("jobs.desc_inact"),      "desc-inact");
        updateJobButton(translateDeltaButton, I18nUtil.get("jobs.translate_delta"), "translate-delta");
        updateJobButton(eszettCheckButton,    I18nUtil.get("jobs.eszett_check"),    "eszett-check");
        updateJobButton(notPublishedButton,   I18nUtil.get("jobs.not_published"),   "not-published");
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
    
    private String getJobDisplayName(String jobType) {
        switch (jobType) {
            case "overview":        return I18nUtil.get("jobs.overview");
            case "desc-add":        return I18nUtil.get("jobs.desc_add");
            case "desc-inact":      return I18nUtil.get("jobs.desc_inact");
            case "translate-delta": return I18nUtil.get("jobs.translate_delta");
            case "eszett-check":    return I18nUtil.get("jobs.eszett_check");
            case "not-published":   return I18nUtil.get("jobs.not_published");
            default:                return jobType;
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
        
        currentFileField.setStyle(currentFileField.getText().isEmpty() ? "-fx-border-color: red;" : "");
        outputDirField.setStyle(outputDirField.getText().isEmpty() ? "-fx-border-color: red;" : "");
    }
    
    private void updateLastJobStatus(JobResult result) {
        String status = result.isSuccessful() ? 
            I18nUtil.get("jobs.status.success") : I18nUtil.get("jobs.status.failed");
        long seconds = result.getExecutionTimeMs() / 1000;
        lastJobLabel.setText(I18nUtil.get("status.lastjob", 
            getJobDisplayName(result.getJobType()), formatDuration(seconds), status));
    }
    
    @FXML
    private void uploadToGitHub() {
        updateConfigFromUI();
        GitHubConfig gitHubConfig = configService.getCurrentConfig().getGithub();

        if (gitHubConfig == null || gitHubConfig.getRepoUrl() == null || gitHubConfig.getRepoUrl().isEmpty()) {
            showWarning(I18nUtil.get("dialog.job.none.title"), 
                       I18nUtil.get("github.dialog.norepo"));
            return;
        }

        String outputDir = configService.getCurrentConfig().getPaths().getOutputDirectory();
        if (outputDir == null || outputDir.isEmpty()) {
            showWarning(I18nUtil.get("dialog.job.none.title"), 
                       I18nUtil.get("github.dialog.nooutput"));
            return;
        }

        if (gitHubUploadService.isUploading()) {
            logMessage(I18nUtil.get("github.upload.inprogress"));
            return;
        }

        Task<String> uploadTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                updateMessage(I18nUtil.get("github.upload.start"));
                return gitHubUploadService.uploadResults(outputDir, gitHubConfig);
            }
        };

        uploadTask.setOnSucceeded(e -> {
            String result = uploadTask.getValue();
            logMessage("\u2705 " + result);
            statusLabel.setText(I18nUtil.get("github.upload.complete"));
            uploadGitHubButton.setDisable(false);
        });

        uploadTask.setOnFailed(e -> {
            Throwable ex = uploadTask.getException();
            String msg = (ex != null) ? ex.getMessage() : "Unknown error";
            logMessage("\u274C " + I18nUtil.get("github.upload.failed") + ": " + msg);
            statusLabel.setText(I18nUtil.get("github.upload.failed.status"));
            uploadGitHubButton.setDisable(false);
        });

        uploadGitHubButton.setDisable(true);
        logMessage(I18nUtil.get("github.upload.starting"));
        statusLabel.setText(I18nUtil.get("github.upload.status.uploading"));

        Thread thread = new Thread(uploadTask);
        thread.setDaemon(true);
        thread.start();
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

        VBox content = new VBox(8);
        content.getChildren().addAll(
            new Label(I18nUtil.get("about.content", year).replace("\\n", "\n")),
            link
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get("about.title"));
        alert.setHeaderText(I18nUtil.get("about.header", "2.0.0"));
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
        fileChooser.setTitle(I18nUtil.get("filechooser.save.log"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18nUtil.get("filter.log"), "*.log", "*.txt")
        );
        fileChooser.setInitialFileName("levi-log.txt");
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                java.nio.file.Files.writeString(file.toPath(), logArea.getText());
                showInfo(I18nUtil.get("success.title"), 
                        I18nUtil.get("log.save.success"));
            } catch (Exception e) {
                logger.error("Error saving log", e);
                showError(I18nUtil.get("error.title"), 
                         I18nUtil.get("log.save.failed", e.getMessage()));
            }
        }
    }
}