package com.springboot.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageConfig {

    /**
     * Root storage directory name under the project root.
     */
    private String rootPath = "storage";

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getResolvedRootPath() {
        return resolveRootPath().toString();
    }

    public String getResolvedRootLocation() {
        return resolveRootPath().toUri().toString();
    }

    private Path resolveRootPath() {
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        Path currentCandidate = workingDirectory.resolve(rootPath).normalize();
        if (Files.exists(currentCandidate)) {
            return currentCandidate;
        }
        Path parentDirectory = workingDirectory.getParent();
        if (parentDirectory != null) {
            Path parentCandidate = parentDirectory.resolve(rootPath).normalize();
            if (Files.exists(parentCandidate)) {
                return parentCandidate;
            }
        }
        try {
            Files.createDirectories(currentCandidate);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory: " + currentCandidate, e);
        }
        return currentCandidate;
    }
}
