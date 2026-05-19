package com.springboot.module.projectwork.eval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * On startup, checks whether Playwright Chromium is already installed.
 * Installs via a separate process to avoid CLI.main() triggering System.exit()
 * which would shut down the Spring context prematurely.
 */
@Slf4j
@Component
public class PlaywrightInstaller implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        Path cacheDir = Path.of(System.getProperty("user.home"), ".cache", "ms-playwright");
        String[] cached = cacheDir.toFile().list();

        if (Files.exists(cacheDir) && cached != null && cached.length > 0) {
            log.info("[Playwright] Chromium already installed at {}, skipping.", cacheDir);
            return;
        }

        log.info("[Playwright] Chromium not found. Installing via subprocess (this takes 1-2 min on first run)...");

        try {
            // Locate the java executable used to launch this JVM
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + "/bin/java";

            // Build classpath from the current JVM's classpath
            String classpath = System.getProperty("java.class.path");

            // Launch CLI.main in a child process so System.exit() only kills that process
            ProcessBuilder pb = new ProcessBuilder(
                    javaBin,
                    "-cp", classpath,
                    "com.microsoft.playwright.CLI",
                    "install", "chromium"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Stream output to log so progress is visible
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Playwright] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("[Playwright] Chromium installed successfully.");
            } else {
                log.error("[Playwright] Chromium install exited with code {}. Screenshots will be unavailable.", exitCode);
            }

        } catch (Exception e) {
            log.error("[Playwright] Chromium install failed: {}. Screenshots will be unavailable.", e.getMessage());
        }
    }
}
