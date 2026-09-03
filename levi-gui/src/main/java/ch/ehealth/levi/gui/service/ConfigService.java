package ch.ehealth.levi.gui.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ch.ehealth.levi.gui.model.AppConfig;
import ch.ehealth.levi.gui.util.EncryptionUtil;
import ch.ehealth.levi.gui.util.I18nUtil;
import ch.ehealth.levi.core.Conf;

/**
 * Service for managing application configuration
 */
public class ConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String DEFAULT_CONFIG_FILE = "levi-config.json";
    private static final String LAST_CONFIG_FILE = ".levi-last-config.json";
    
    private final ObjectMapper objectMapper;
    private AppConfig currentConfig;
    private Path lastConfigPath;
    
    public ConfigService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Determine last config path (user home directory)
        String userHome = System.getProperty("user.home");
        this.lastConfigPath = Paths.get(userHome, LAST_CONFIG_FILE);
        
        // Initialize with default config
        this.currentConfig = new AppConfig();
    }
    
    /**
     * Saves configuration to a file
     * 
     * @param file the file to save to
     * @throws IOException if saving fails
     */
    public void saveConfig(File file) throws IOException {
        // Encrypt password before saving
        AppConfig configToSave = cloneConfig(currentConfig);
        String plainPassword = configToSave.getDatabase().getPassword();
        if (plainPassword != null && !plainPassword.isEmpty()) {
            String encryptedPassword = EncryptionUtil.encrypt(plainPassword);
            configToSave.getDatabase().setPassword(encryptedPassword);
        }

        // Encrypt GitHub token before saving
        String plainToken = configToSave.getGithub().getToken();
        if (plainToken != null && !plainToken.isEmpty()) {
            String encryptedToken = EncryptionUtil.encrypt(plainToken);
            configToSave.getGithub().setToken(encryptedToken);
        }

        objectMapper.writeValue(file, configToSave);
        logger.info("Configuration saved to: {}", file.getAbsolutePath());
        
        // Also save as last config (only if this isn't already the last-config file)
        if (!file.toPath().equals(lastConfigPath)) {
            saveLastConfig();
        }
    }
    
    /**
     * Loads configuration from a file
     * 
     * @param file the file to load from
     * @throws IOException if loading fails
     */
    public void loadConfig(File file) throws IOException {
        AppConfig loadedConfig = objectMapper.readValue(file, AppConfig.class);
        
        // Decrypt password after loading
        String encryptedPassword = loadedConfig.getDatabase().getPassword();
        if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
            try {
                String plainPassword = EncryptionUtil.decrypt(encryptedPassword);
                loadedConfig.getDatabase().setPassword(plainPassword);
            } catch (Exception e) {
                logger.error("Failed to decrypt DB password", e);
                throw new IOException("Failed to decrypt database password", e);
            }
        }

        // Decrypt GitHub token after loading
        if (loadedConfig.getGithub() != null) {
            String encryptedToken = loadedConfig.getGithub().getToken();
            if (encryptedToken != null && !encryptedToken.isEmpty()) {
                try {
                    String plainToken = EncryptionUtil.decrypt(encryptedToken);
                    loadedConfig.getGithub().setToken(plainToken);
                } catch (Exception e) {
                    logger.error("Failed to decrypt GitHub token: '{}'", encryptedToken, e);
                    throw new IOException("Failed to decrypt GitHub token", e);
                }
            }
        }
        
        this.currentConfig = loadedConfig;
        logger.info("Configuration loaded from: {}", file.getAbsolutePath());
        
        // Save as last config (only if this isn't already the last-config file)
        if (!file.toPath().equals(lastConfigPath)) {
            saveLastConfig();
        }
    }
    
    /**
     * Loads the last used configuration
     * 
     * @return true if last config was loaded successfully
     */
    public boolean loadLastConfig() {
        if (Files.exists(lastConfigPath)) {
            try {
                loadConfig(lastConfigPath.toFile());
                return true;
            } catch (IOException e) {
                logger.warn("Could not load last configuration", e);
            }
        }
        return false;
    }
    
    /**
     * Saves the current configuration as last config
     */
    private void saveLastConfig() {
        try {
            saveConfig(lastConfigPath.toFile());
        } catch (IOException e) {
            logger.warn("Could not save last configuration", e);
        }
    }
    
    /**
     * Restores default configuration
     */
    public void restoreDefaults() {
        this.currentConfig = new AppConfig();
        logger.info("Configuration restored to defaults");
    }
    
    /**
     * Gets the current configuration
     * 
     * @return current configuration
     */
    public AppConfig getCurrentConfig() {
        return currentConfig;
    }
    
    /**
     * Sets the current configuration
     * 
     * @param config the new configuration
     */
    public void setCurrentConfig(AppConfig config) {
        this.currentConfig = config;
    }
    
    /**
     * Converts AppConfig to Conf (for LEVI core)
     * 
     * @return Conf object
     */
    public Conf toConf() {
        Conf conf = new Conf();
        
        // Database settings
        conf.setDbUrl(currentConfig.getDatabase().getUrl());
        conf.setDbUsername(currentConfig.getDatabase().getUsername());
        conf.setDbPassword(currentConfig.getDatabase().getPassword());
        
        // Settings
        conf.setCountryCode(currentConfig.getSettings().getCountryCode());
        conf.setLanguageCodeFilter(currentConfig.getSettings().getLanguageCodeFilter());
        conf.setTransformEszett(currentConfig.getSettings().isTransformEszett());
        conf.setRegexCheck(currentConfig.getSettings().isRegexCheck());
        conf.setGroupingEnabled(currentConfig.getSettings().isGrouping());
        conf.setLexiconDir(currentConfig.getSettings().getLexiconDir());
        
        // Paths
        conf.setFilePathCurrent(currentConfig.getPaths().getCurrentFile());
        conf.setFilePathPrevious(currentConfig.getPaths().getPreviousFile());
        conf.setDestination(currentConfig.getPaths().getOutputDirectory());
        conf.setFrDescriptionPath(currentConfig.getPaths().getFrDescriptionPath());
        conf.setChDescriptionPath(currentConfig.getPaths().getChDescriptionPath());
        conf.setFrLanguageRefsetPath(currentConfig.getPaths().getFrLanguageRefsetPath());
        conf.setChLanguageRefsetPath(currentConfig.getPaths().getChLanguageRefsetPath());
        
        return conf;
    }
    
    /**
     * Validates the current configuration
     * 
     * @return validation error message or null if valid
     */
    public String validateConfig() {
        if (currentConfig.getDatabase().getDbName() == null || currentConfig.getDatabase().getDbName().isEmpty()) {
            return I18nUtil.get("validation.dbname");
        }
        
        if (currentConfig.getPaths().getCurrentFile() == null || currentConfig.getPaths().getCurrentFile().isEmpty()) {
            return I18nUtil.get("validation.currentfile");
        }
        
        if (currentConfig.getPaths().getOutputDirectory() == null || currentConfig.getPaths().getOutputDirectory().isEmpty()) {
            return I18nUtil.get("validation.outputdir");
        }
        
        // Check if current file exists
        File currentFile = new File(currentConfig.getPaths().getCurrentFile());
        if (!currentFile.exists()) {
            return I18nUtil.get("validation.filenotexist", currentFile.getAbsolutePath());
        }
        
        // Check if output directory exists or can be created
        File outputDir = new File(currentConfig.getPaths().getOutputDirectory());
        if (outputDir.exists() && !outputDir.isDirectory()) {
            return I18nUtil.get("validation.outputnotdir", outputDir.getAbsolutePath());
        }
        
        // Validate GitHub config if auto-upload is enabled
        if (currentConfig.getGithub() != null && currentConfig.getGithub().isAutoUpload()) {
            if (currentConfig.getGithub().getRepoUrl() == null || currentConfig.getGithub().getRepoUrl().isEmpty()) {
                return I18nUtil.get("validation.github.repo");
            }
            if (currentConfig.getGithub().getToken() == null || currentConfig.getGithub().getToken().isEmpty()) {
                return I18nUtil.get("validation.github.token");
            }
        }
        
        return null; // Valid
    }
    
    /**
     * Clones an AppConfig object
     */
    private AppConfig cloneConfig(AppConfig config) throws IOException {
        String json = objectMapper.writeValueAsString(config);
        return objectMapper.readValue(json, AppConfig.class);
    }
}