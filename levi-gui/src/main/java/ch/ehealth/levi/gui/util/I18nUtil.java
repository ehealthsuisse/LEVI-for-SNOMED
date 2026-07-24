package ch.ehealth.levi.gui.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class for internationalization (i18n) support
 */
public class I18nUtil {
    
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static final String LANGUAGE_PREF_FILE = ".levi-language";
    private static final Path LANGUAGE_PREF_PATH;
    
    private static Locale currentLocale = Locale.ENGLISH;
    private static ResourceBundle resourceBundle;
    
    static {
        LANGUAGE_PREF_PATH = Paths.get(System.getProperty("user.home"), LANGUAGE_PREF_FILE);
        loadSavedLocale();
        loadResourceBundle();
    }
    
    /**
     * Gets a localized string for the given key
     * 
     * @param key the resource key
     * @return localized string
     */
    public static String get(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }
    
    /**
     * Gets a localized string with parameters
     * 
     * @param key the resource key
     * @param params parameters to format into the string
     * @return formatted localized string
     */
    public static String get(String key, Object... params) {
        try {
            String template = resourceBundle.getString(key);
            return String.format(template, params);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }
    
    /**
     * Sets the current locale
     * 
     * @param locale the new locale
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        loadResourceBundle();
    }
    
    /**
     * Gets the current locale
     * 
     * @return current locale
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * Gets the current resource bundle
     * 
     * @return current resource bundle
     */
    public static ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
    
    /**
     * Sets locale by language code
     * 
     * @param languageCode language code (de, en, fr, it)
     */
    public static void setLocaleByLanguageCode(String languageCode) {
        Locale locale;
        switch (languageCode.toLowerCase()) {
            case "de":
                locale = Locale.GERMAN;
                break;
            case "en":
                locale = Locale.ENGLISH;
                break;
            case "fr":
                locale = Locale.FRENCH;
                break;
            case "it":
                locale = Locale.ITALIAN;
                break;
            default:
                locale = Locale.ENGLISH;
        }
        setLocale(locale);
    }
    
    /**
     * Persists the chosen language so it survives restarts
     */
    public static void saveLanguagePreference(String languageCode) {
        try {
            Files.writeString(LANGUAGE_PREF_PATH, languageCode);
        } catch (IOException e) {
            // silent — preference is optional
        }
    }
    
    /**
     * Loads a previously saved language preference from disk
     */
    private static void loadSavedLocale() {
        try {
            if (Files.exists(LANGUAGE_PREF_PATH)) {
                String lang = Files.readString(LANGUAGE_PREF_PATH).trim();
                switch (lang.toLowerCase()) {
                    case "de": currentLocale = Locale.GERMAN; return;
                    case "en": currentLocale = Locale.ENGLISH; return;
                    case "fr": currentLocale = Locale.FRENCH; return;
                    case "it": currentLocale = Locale.ITALIAN; return;
                    default: currentLocale = Locale.ENGLISH; return;
                }
            }
        } catch (IOException e) {
            // silent — fall back to default
        }
        currentLocale = Locale.ENGLISH;
    }
    
    /**
     * Loads the resource bundle for the current locale
     */
    private static void loadResourceBundle() {
        try {
            resourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
        } catch (Exception e) {
            // Fallback to English
            resourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);
        }
    }
}