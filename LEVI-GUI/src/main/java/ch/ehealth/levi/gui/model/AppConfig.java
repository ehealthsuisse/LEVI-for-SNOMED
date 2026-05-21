package ch.ehealth.levi.gui.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Application configuration model for JSON serialization
 */
public class AppConfig {
    
    @JsonProperty("version")
    private String version = "1.0";
    
    @JsonProperty("database")
    private DatabaseConfig database;
    
    @JsonProperty("settings")
    private Settings settings;
    
    @JsonProperty("paths")
    private Paths paths;
    
    public AppConfig() {
        this.database = new DatabaseConfig();
        this.settings = new Settings();
        this.paths = new Paths();
    }
    
    // Getters and setters
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public DatabaseConfig getDatabase() {
        return database;
    }
    
    public void setDatabase(DatabaseConfig database) {
        this.database = database;
    }
    
    public Settings getSettings() {
        return settings;
    }
    
    public void setSettings(Settings settings) {
        this.settings = settings;
    }
    
    public Paths getPaths() {
        return paths;
    }
    
    public void setPaths(Paths paths) {
        this.paths = paths;
    }
    
    public static class DatabaseConfig {
        @JsonProperty("dbName")
        private String dbName = "SCT:CH_Dec25";

        @JsonProperty("dbPort")
        private int dbPort = 3306;
        
        @JsonProperty("username")
        private String username = "root";
        
        @JsonProperty("password")
        private String password = "";

        /** Returns the full JDBC URL constructed from host, port and DB name. */
        @com.fasterxml.jackson.annotation.JsonIgnore
        public String getUrl() {
            return "jdbc:mysql://localhost:" + dbPort + "/" + dbName
                    + "?useUnicode=true&characterEncoding=UTF-8";
        }

        public String getDbName() {
            return dbName;
        }

        public void setDbName(String dbName) {
            this.dbName = dbName;
        }

        public int getDbPort() {
            return dbPort;
        }

        public void setDbPort(int dbPort) {
            this.dbPort = dbPort;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    public static class Settings {
        @JsonProperty("countryCode")
        private String countryCode = "CH";
        
        @JsonProperty("transformEszett")
        private boolean transformEszett = true;
        
        @JsonProperty("regexCheck")
        private boolean regexCheck = true;
        
        public String getCountryCode() {
            return countryCode;
        }
        
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
        
        public boolean isTransformEszett() {
            return transformEszett;
        }
        
        public void setTransformEszett(boolean transformEszett) {
            this.transformEszett = transformEszett;
        }
        
        public boolean isRegexCheck() {
            return regexCheck;
        }
        
        public void setRegexCheck(boolean regexCheck) {
            this.regexCheck = regexCheck;
        }
    }
    
    public static class Paths {
        @JsonProperty("currentFile")
        private String currentFile = "";
        
        @JsonProperty("previousFile")
        private String previousFile = "";
        
        @JsonProperty("outputDirectory")
        private String outputDirectory = "";
        
        public String getCurrentFile() {
            return currentFile;
        }
        
        public void setCurrentFile(String currentFile) {
            this.currentFile = currentFile;
        }
        
        public String getPreviousFile() {
            return previousFile;
        }
        
        public void setPreviousFile(String previousFile) {
            this.previousFile = previousFile;
        }
        
        public String getOutputDirectory() {
            return outputDirectory;
        }
        
        public void setOutputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
        }
    }
}
