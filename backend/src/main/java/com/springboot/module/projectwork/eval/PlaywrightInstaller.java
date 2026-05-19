package com.springboot.module.projectwork.eval;

import com.microsoft.playwright.CLI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * On startup, checks whether Playwright Chromium is already installed.
 * If not, downloads it automatically. Subsequent startups skip the download.
 */
@Slf4j
@Component
public class PlaywrightInstaller implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        Path playwrightCacheDir = Path.of(
                System.getProperty("user.home"), ".cache", "ms-playwright");

        String[] cached = playwrightCacheDir.toFile().list();
        if (Files.exists(playwrightCacheDir) && cached != null && cached.length > 0) {
            log.info("[Playwright] Browser already installed at {}", playwrightCacheDir);
            return;
        }

        log.info("[Playwright] Browser not found, installing Chromium (first time ~1-2 min)...");
        try {
            CLI.main(new String[]{"install", "chromium"});
            log.info("[Playwright] Chromium installed successfully.");
        } catch (Exception e) {
            log.error("[Playwright] Chromium install failed, screenshots will be unavailable: {}",
                    e.getMessage());
        }
    }
}
