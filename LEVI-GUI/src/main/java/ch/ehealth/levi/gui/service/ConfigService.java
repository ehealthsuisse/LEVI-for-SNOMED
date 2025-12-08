package ch.ehealth.levi.gui.service;

import ch.ehealth.levi.gui.model.AppConfig;
import ch.ehealth.levi.gui.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import translation.check.Conf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        
        objectMapper.writeValue(file, configToSave);
        logger.info("Configuration saved to: {}", file.getAbsolutePath());
        
        // Also save as last config
        saveLastConfig();
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
            String plainPassword = EncryptionUtil.decrypt(encryptedPassword);
            loadedConfig.getDatabase().setPassword(plainPassword);
        }
        
        this.currentConfig = loadedConfig;
        logger.info("Configuration loaded from: {}", file.getAbsolutePath());
        
        // Save as last config
        saveLastConfig();
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
        conf.setTransformEszett(currentConfig.getSettings().isTransformEszett());
        conf.setRegexCheck(currentConfig.getSettings().isRegexCheck());
        
        // Paths
        conf.setFilePathCurrent(currentConfig.getPaths().getCurrentFile());
        conf.setFilePathPrevious(currentConfig.getPaths().getPreviousFile());
        conf.setDestination(currentConfig.getPaths().getOutputDirectory());
        
        return conf;
    }
    
    /**
     * Validates the current configuration
     * 
     * @return validation error message or null if valid
     */
    public String validateConfig() {
        if (currentConfig.getDatabase().getUrl() == null || currentConfig.getDatabase().getUrl().isEmpty()) {
            return "Database URL is required";
        }
        
        if (currentConfig.getPaths().getCurrentFile() == null || currentConfig.getPaths().getCurrentFile().isEmpty()) {
            return "Current file path is required";
        }
        
        if (currentConfig.getPaths().getOutputDirectory() == null || currentConfig.getPaths().getOutputDirectory().isEmpty()) {
            return "Output directory is required";
        }
        
        // Check if current file exists
        File currentFile = new File(currentConfig.getPaths().getCurrentFile());
        if (!currentFile.exists()) {
            return "Current file does not exist: " + currentFile.getAbsolutePath();
        }
        
        // Check if output directory exists or can be created
        File outputDir = new File(currentConfig.getPaths().getOutputDirectory());
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                return "Cannot create output directory: " + outputDir.getAbsolutePath();
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
