package ch.ehealth.levi.core.db;

/**
 * Configuration for creating a SNOMED CT database from RF2 release files.
 *
 * <p>Supports the same scenarios as the standalone SNOMED_Database project:
 * International Edition + an extension (CH production, CH beta, CH pre-production
 * or AT), with Full or Snapshot release files.
 */
public class DbCreateConfig {

    public enum ReleaseType {
        FULL("Full"),
        SNAPSHOT("Snapshot");

        private final String folderName;

        ReleaseType(String folderName) {
            this.folderName = folderName;
        }

        public String getFolderName() {
            return folderName;
        }
    }

    private String dbName = "SCT:CH_Jun26";
    private String dbHost = "localhost";
    private int dbPort = 3306;
    private String dbUser = "root";
    private String dbPassword = "";

    private String intlReleasePath;
    private String extensionReleasePath;
    private String countryCode = "CH";
    private ReleaseType releaseType = ReleaseType.FULL;

    /** Database variant (PRODUCTION, BETA, PRE_PRODUCTION, AT); informational. */
    private String dbVariant = DbVariant.PRODUCTION.name();

    /** Optional explicit international release date (e.g. "20260601"). When null,
     *  the date is derived from the detected release files. */
    private String intlReleaseDate;

    /** Optional explicit extension module identifier (e.g. "CH1000195_20260607").
     *  When null, the module is derived from the detected release files. */
    private String extensionModuleId;

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

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

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public ReleaseType getReleaseType() {
        return releaseType;
    }

    public void setReleaseType(ReleaseType releaseType) {
        this.releaseType = releaseType;
    }

    public String getDbVariant() {
        return dbVariant;
    }

    public void setDbVariant(String dbVariant) {
        this.dbVariant = dbVariant;
    }

    public String getIntlReleaseDate() {
        return intlReleaseDate;
    }

    public void setIntlReleaseDate(String intlReleaseDate) {
        this.intlReleaseDate = intlReleaseDate;
    }

    public String getExtensionModuleId() {
        return extensionModuleId;
    }

    public void setExtensionModuleId(String extensionModuleId) {
        this.extensionModuleId = extensionModuleId;
    }

    /** JDBC URL for connecting to the MySQL server without a database selected. */
    public String getServerUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort
                + "/?allowMultiQueries=true&allowLoadLocalInfile=true";
    }

    /** JDBC URL for connecting to the newly created database. */
    public String getDatabaseUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                + "?allowMultiQueries=true&allowLoadLocalInfile=true";
    }
}
