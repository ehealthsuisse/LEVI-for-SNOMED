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

    @JsonProperty("github")
    private GitHubConfig github;

    @JsonProperty("dbSetup")
    private DbSetupConfig dbSetup;

    @JsonProperty("xampp")
    private XamppConfig xampp;

    public AppConfig() {
        this.database = new DatabaseConfig();
        this.settings = new Settings();
        this.paths = new Paths();
        this.github = new GitHubConfig();
        this.dbSetup = new DbSetupConfig();
        this.xampp = new XamppConfig();
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

    public GitHubConfig getGithub() {
        return github;
    }

    public void setGithub(GitHubConfig github) {
        this.github = github;
    }

    public DbSetupConfig getDbSetup() {
        return dbSetup;
    }

    public void setDbSetup(DbSetupConfig dbSetup) {
        this.dbSetup = dbSetup;
    }

    public XamppConfig getXampp() {
        return xampp;
    }

    public void setXampp(XamppConfig xampp) {
        this.xampp = xampp;
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
        
        @JsonProperty("languageCodeFilter")
        private String languageCodeFilter = "";
        
        @JsonProperty("transformEszett")
        private boolean transformEszett = true;
        
        @JsonProperty("regexCheck")
        private boolean regexCheck = true;
        
        @JsonProperty("grouping")
        private boolean grouping = true;

        @JsonProperty("lexiconDir")
        private String lexiconDir = "";

        public String getCountryCode() {
            return countryCode;
        }
        
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
        
        public String getLanguageCodeFilter() {
            return languageCodeFilter;
        }
        
        public void setLanguageCodeFilter(String languageCodeFilter) {
            this.languageCodeFilter = languageCodeFilter;
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
        
        public boolean isGrouping() {
            return grouping;
        }
        
        public void setGrouping(boolean grouping) {
            this.grouping = grouping;
        }

        public String getLexiconDir() {
            return lexiconDir;
        }

        public void setLexiconDir(String lexiconDir) {
            this.lexiconDir = lexiconDir;
        }
    }
    
    public static class Paths {
        @JsonProperty("currentFile")
        private String currentFile = "";
        
        @JsonProperty("previousFile")
        private String previousFile = "";
        
        @JsonProperty("outputDirectory")
        private String outputDirectory = "";

        @JsonProperty("frDescriptionPath")
        private String frDescriptionPath = "";

        @JsonProperty("chDescriptionPath")
        private String chDescriptionPath = "";

        @JsonProperty("frLanguageRefsetPath")
        private String frLanguageRefsetPath = "";

        @JsonProperty("chLanguageRefsetPath")
        private String chLanguageRefsetPath = "";
        
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

        public String getFrDescriptionPath() {
            return frDescriptionPath;
        }

        public void setFrDescriptionPath(String frDescriptionPath) {
            this.frDescriptionPath = frDescriptionPath;
        }

        public String getChDescriptionPath() {
            return chDescriptionPath;
        }

        public void setChDescriptionPath(String chDescriptionPath) {
            this.chDescriptionPath = chDescriptionPath;
        }

        public String getFrLanguageRefsetPath() {
            return frLanguageRefsetPath;
        }

        public void setFrLanguageRefsetPath(String frLanguageRefsetPath) {
            this.frLanguageRefsetPath = frLanguageRefsetPath;
        }

        public String getChLanguageRefsetPath() {
            return chLanguageRefsetPath;
        }

        public void setChLanguageRefsetPath(String chLanguageRefsetPath) {
            this.chLanguageRefsetPath = chLanguageRefsetPath;
        }
    }

    public static class GitHubConfig {
        @JsonProperty("repoUrl")
        private String repoUrl = "";

        @JsonProperty("branch")
        private String branch = "results";

        @JsonProperty("token")
        private String token = "";

        @JsonProperty("autoUpload")
        private boolean autoUpload = false;

        public String getRepoUrl() {
            return repoUrl;
        }

        public void setRepoUrl(String repoUrl) {
            this.repoUrl = repoUrl;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public boolean isAutoUpload() {
            return autoUpload;
        }

        public void setAutoUpload(boolean autoUpload) {
            this.autoUpload = autoUpload;
        }
    }

    /** Settings for the SNOMED DB creation feature (Database Setup tab). */
    public static class DbSetupConfig {
        @JsonProperty("intlReleasePath")
        private String intlReleasePath = "";

        @JsonProperty("extensionReleasePath")
        private String extensionReleasePath = "";

        @JsonProperty("dbNameToCreate")
        private String dbNameToCreate = "SCT:CH_Jun26";

        @JsonProperty("releaseType")
        private String releaseType = "FULL";

        @JsonProperty("countryCode")
        private String countryCode = "CH";

        @JsonProperty("dbVariant")
        private String dbVariant = "PRODUCTION";

        public String getIntlReleasePath() {
            return intlReleasePath;
        }

        public void setIntlReleasePath(String intlReleasePath) {
            this.intlReleasePath = intlReleasePath;
        }

        public String getExtensionReleasePath() {
            return extensionReleasePath;
        }

        public void setExtensionReleasePath(String extensionReleasePath) {
            this.extensionReleasePath = extensionReleasePath;
        }

        public String getDbNameToCreate() {
            return dbNameToCreate;
        }

        public void setDbNameToCreate(String dbNameToCreate) {
            this.dbNameToCreate = dbNameToCreate;
        }

        public String getReleaseType() {
            return releaseType;
        }

        public void setReleaseType(String releaseType) {
            this.releaseType = releaseType;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getDbVariant() {
            return dbVariant;
        }

        public void setDbVariant(String dbVariant) {
            this.dbVariant = dbVariant;
        }
    }

    /** Settings for the XAMPP server control (Database Setup tab). */
    public static class XamppConfig {
        @JsonProperty("lamppPath")
        private String lamppPath = ch.ehealth.levi.gui.service.XamppService.DEFAULT_LAMPP_PATH;

        public String getLamppPath() {
            return lamppPath;
        }

        public void setLamppPath(String lamppPath) {
            this.lamppPath = lamppPath;
        }
    }
}
