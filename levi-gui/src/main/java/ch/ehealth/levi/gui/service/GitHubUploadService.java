package ch.ehealth.levi.gui.service;

import ch.ehealth.levi.gui.model.AppConfig.GitHubConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

public class GitHubUploadService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubUploadService.class);
    private static final String CACHE_DIR = System.getProperty("user.home") + "/.levi-github-cache";

    private boolean uploading = false;

    public boolean isUploading() {
        return uploading;
    }

    public String uploadResults(String outputDirPath, GitHubConfig config) throws Exception {
        if (config.getRepoUrl() == null || config.getRepoUrl().trim().isEmpty()) {
            return "No repository URL configured";
        }
        if (config.getToken() == null || config.getToken().trim().isEmpty()) {
            return "No GitHub token configured";
        }

        configureProxy();

        uploading = true;
        try {
            String repoInput = config.getRepoUrl().trim();
            String token = config.getToken().trim();

            String repoUrl;
            if (repoInput.startsWith("http://") || repoInput.startsWith("https://")) {
                repoUrl = repoInput;
            } else {
                repoUrl = "https://github.com/" + repoInput + ".git";
            }

            CredentialsProvider cp = new UsernamePasswordCredentialsProvider(token, "");

            String branchInput = (config.getBranch() != null) ? config.getBranch().trim() : "";

            String repoCacheName = repoUrl.replace('/', '-').replace(':', '-')
                    .replace("https", "").replace("http", "");
            Path cacheDir = Paths.get(CACHE_DIR, repoCacheName);

            deleteDirectory(cacheDir);

            logger.info("Cloning repository...");
            Git git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(cacheDir.toFile())
                    .setCredentialsProvider(cp)
                    .call();

            String resolvedBranch;
            if (!branchInput.isEmpty()) {
                try {
                    git.checkout()
                            .setCreateBranch(true)
                            .setName(branchInput)
                            .setStartPoint("origin/" + branchInput)
                            .setForce(true)
                            .call();
                    resolvedBranch = branchInput;
                } catch (RefNotFoundException e) {
                    logger.info("Branch '{}' does not exist on remote, creating from HEAD", branchInput);
                    git.checkout()
                            .setCreateBranch(true)
                            .setName(branchInput)
                            .call();
                    resolvedBranch = branchInput;
                }
            } else {
                resolvedBranch = git.getRepository().getBranch();
            }

            logger.info("Using branch: {}", resolvedBranch);

            removeAllFilesExceptGit(cacheDir);

            Path outputDir = Paths.get(outputDirPath);
            if (!Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
                return "Output directory does not exist: " + outputDirPath;
            }

            int fileCount = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir, "*.tsv")) {
                for (Path tsvFile : stream) {
                    Files.copy(tsvFile, cacheDir.resolve(tsvFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    fileCount++;
                }
            }

            if (fileCount == 0) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir, "*.xlsx")) {
                    for (Path xlsxFile : stream) {
                        Files.copy(xlsxFile, cacheDir.resolve(xlsxFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                        fileCount++;
                    }
                }
            }

            if (fileCount == 0) {
                return "No TSV or XLSX files found in: " + outputDirPath;
            }

            git.add().addFilepattern(".").call();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            git.commit()
                    .setMessage("LEVI results " + timestamp)
                    .setAuthor("LEVI for SNOMED", "levi@ehealth-suisse.ch")
                    .setCommitter("LEVI for SNOMED", "levi@ehealth-suisse.ch")
                    .call();

            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(cp)
                    .add(resolvedBranch)
                    .call();

            git.close();

            return String.format("Uploaded %d files to %s (branch: %s)", fileCount, repoUrl, resolvedBranch);
        } finally {
            uploading = false;
        }
    }

    private void configureProxy() {
        String proxyUrl = firstNonEmpty(
                System.getenv("https_proxy"),
                System.getenv("HTTPS_PROXY"),
                System.getenv("http_proxy"),
                System.getenv("HTTP_PROXY")
        );

        if (proxyUrl == null || proxyUrl.isEmpty()) {
            logger.info("No proxy configured in environment");
            return;
        }

        try {
            java.net.URI uri = java.net.URI.create(proxyUrl);
            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 8080;

            System.setProperty("https.proxyHost", host);
            System.setProperty("https.proxyPort", String.valueOf(port));
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", String.valueOf(port));

            
            String noProxy = System.getenv("no_proxy");
            if (noProxy == null) noProxy = System.getenv("NO_PROXY");
            if (noProxy != null && !noProxy.isEmpty()) {
                String nonProxyHosts = noProxy.replace(",", "|").replace(".", "*.");
                System.setProperty("http.nonProxyHosts", nonProxyHosts);
            }

            logger.info("Using proxy: {}:{}", host, port);
        } catch (Exception e) {
            logger.warn("Could not parse proxy URL: {}", proxyUrl, e);
        }
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }
    
    private void removeAllFilesExceptGit(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(dir))
                    .filter(p -> !p.getFileName().toString().equals(".git"))
                    .filter(p -> !p.startsWith(dir.resolve(".git")))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.warn("Could not delete: {}", p);
                        }
                    });
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }
}
