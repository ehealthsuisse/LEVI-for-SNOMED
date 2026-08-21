package ch.ehealth.levi.gui.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for controlling the XAMPP server from LEVI.
 *
 * <p>The XAMPP control script requires root privileges, so the sudo password
 * entered by the user is piped to {@code sudo -S} on standard input. The
 * password is used transiently in memory only and is never stored or logged.
 */
public class XamppService {

    private static final Logger logger = LoggerFactory.getLogger(XamppService.class);

    /** Default XAMPP control script location on Linux. */
    public static final String DEFAULT_LAMPP_PATH = "/opt/lampp/lampp";

    /** Result of a XAMPP control command. */
    public static class XamppCommandResult {
        private final int exitCode;
        private final String output;

        public XamppCommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    private final String lamppPath;

    public XamppService() {
        this(DEFAULT_LAMPP_PATH);
    }

    public XamppService(String lamppPath) {
        this.lamppPath = lamppPath;
    }

    /**
     * @return true when the configured XAMPP control script exists on this system
     */
    public boolean isAvailable() {
        return new File(lamppPath).exists();
    }

    public String getLamppPath() {
        return lamppPath;
    }

    /**
     * Checks whether a MySQL/MariaDB server is reachable on the given host/port
     * with the given credentials (no root privileges required).
     */
    public boolean isMysqlReachable(String host, int port, String user, String password) {
        String url = "jdbc:mysql://" + host + ":" + port + "/?connectTimeout=2000";
        try {
            try (var conn = DriverManager.getConnection(url, user, password)) {
                return conn.isValid(2);
            }
        } catch (SQLException | RuntimeException e) {
            logger.debug("MySQL not reachable at {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }

    /**
     * Runs a XAMPP control action (start, stop, status, restart, etc.) using
     * {@code sudo -S}, feeding the password through standard input.
     *
     * @param action        the lampp action, e.g. "start", "stop", "status"
     * @param sudoPassword  the sudo password (may be empty for passwordless sudo)
     * @return the command result with exit code and combined output
     * @throws IllegalStateException if the lampp script is not available
     */
    public XamppCommandResult runCommand(String action, String sudoPassword) {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "XAMPP control script not found: " + lamppPath
                            + ". Not available on this system?");
        }

        ProcessBuilder pb = new ProcessBuilder("sudo", "-S", "-p", "",
                lamppPath, action);
        pb.redirectErrorStream(true);

        try {
            Process proc = pb.start();
            String pwd = sudoPassword == null ? "" : sudoPassword;
            try (OutputStream stdin = proc.getOutputStream()) {
                stdin.write((pwd + "\n").getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            boolean finished = proc.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new IllegalStateException("Timed out waiting for lampp " + action);
            }
            return new XamppCommandResult(proc.exitValue(), output.toString().trim());
        } catch (java.io.IOException e) {
            logger.error("Failed to execute lampp " + action, e);
            throw new IllegalStateException("Failed to execute lampp " + action
                    + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running lampp " + action, e);
        }
    }
}