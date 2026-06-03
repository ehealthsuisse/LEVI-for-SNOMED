package ch.ehealth.levi.gui.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A Logback appender that forwards log messages to the GUI's log TextArea.
 *
 * Rules:
 *  - All messages from translation.check.* are shown (the LEVI core).
 *  - WARN and ERROR from any other logger are shown (so GUI errors are visible).
 *  - Everything else is suppressed to keep the log area readable.
 *
 * Wire the TextArea once at startup via {@link #setLogArea(TextArea)}.
 */
public class GuiLogAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Shared reference set by MainController.initialize(). */
    private static volatile TextArea logArea;

    public static void setLogArea(TextArea area) {
        logArea = area;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (logArea == null) return;

        String loggerName = event.getLoggerName();
        boolean isLeviCore = loggerName.startsWith("translation.check");
        boolean isWarnOrAbove = event.getLevel().isGreaterOrEqual(Level.WARN);

        if (!isLeviCore && !isWarnOrAbove) return;

        String time = LocalTime.ofInstant(
                Instant.ofEpochMilli(event.getTimeStamp()),
                ZoneId.systemDefault()).format(TIME_FMT);

        // Short logger name (last segment after the last dot)
        int dot = loggerName.lastIndexOf('.');
        String shortName = dot >= 0 ? loggerName.substring(dot + 1) : loggerName;

        String level = event.getLevel().toString();
        String line = "[" + time + "] " + level + " " + shortName
                + " - " + event.getFormattedMessage();

        try {
            Platform.runLater(() -> logArea.appendText(line + "\n"));
        } catch (IllegalStateException ignored) {
            // Platform not yet initialised; drop the message silently
        }
    }
}
