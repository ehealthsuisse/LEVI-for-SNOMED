package ch.ehealth.levi.gui;

import ch.ehealth.levi.gui.controller.MainController;
import ch.ehealth.levi.gui.util.I18nUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX application class for LEVI GUI
 */
public class LeviGuiApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(LeviGuiApplication.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting LEVI GUI Application");
            
            // Load FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/MainView.fxml"));
            loader.setResources(I18nUtil.getResourceBundle());
            
            // Set controller
            MainController controller = new MainController();
            loader.setController(controller);
            
            Parent root = loader.load();
            
            // Set up scene
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Set up stage
            primaryStage.setTitle(I18nUtil.get("app.title"));
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            // Set window icons (multiple sizes for OS taskbar / dock)
            for (String size : new String[]{"16", "32", "192", "512"}) {
                String name = size.equals("16") || size.equals("32")
                        ? "/icons/favicon-" + size + "x" + size + ".png"
                        : "/icons/android-chrome-" + size + "x" + size + ".png";
                try (java.io.InputStream is = getClass().getResourceAsStream(name)) {
                    if (is != null) primaryStage.getIcons().add(new Image(is));
                } catch (Exception ignored) {}
            }
            
            // Initialize controller with stage
            controller.setStage(primaryStage);
            
            primaryStage.show();
            
            logger.info("LEVI GUI Application started successfully");
        } catch (Exception e) {
            logger.error("Error starting LEVI GUI Application", e);
            throw new RuntimeException("Failed to start application", e);
        }
    }
    
    @Override
    public void stop() {
        logger.info("Stopping LEVI GUI Application");
    }
    
    public static void main(String[] args) {
        // Set macOS Dock icon programmatically (fallback when -Xdock:icon is not set)
        try {
            if (java.awt.Taskbar.isTaskbarSupported()) {
                java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    java.net.URL iconUrl = LeviGuiApplication.class
                            .getResource("/icons/android-chrome-512x512.png");
                    if (iconUrl != null) {
                        taskbar.setIconImage(
                                java.awt.Toolkit.getDefaultToolkit().getImage(iconUrl));
                    }
                }
            }
        } catch (Exception ignored) {}
        launch(args);
    }
}
