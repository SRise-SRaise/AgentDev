package com.springboot.module.projectwork.eval;

import com.microsoft.playwright.CLI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 应用启动时自动检测并安装 Playwright Chromium 浏览器。
 * 若浏览器目录已存在则跳过，避免每次启动都重新下载。
 */
@Slf4j
@Component
public class PlaywrightInstaller implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // Playwright 默认把浏览器装在 ~/.cache/ms-playwright
        Path playwrightCacheDir = Path.of(System.getProperty("user.home"), ".cache", "ms-playwright");

        if (Files.exists(playwrightCacheDir) && playwrightCacheDir.toFile().list() != null
                && playwrightCacheDir.toFile().list().length > 0) {
            log.info("[Playwright] 浏览器已安装，跳过下载。路径: {}", playwrightCacheDir);
            return;
        }

        log.info("[Playwright] 未检测到浏览器，开始安装 Chromium（首次约需 1~2 分钟）...");
        try {
            CLI.main(new String[]{"install", "chromium"});
            log.info("[Playwright] Chromium 安装完成。");
        } catch (Exception e) {
            log.error("[Playwright] Chromium 安装失败，截图功能将不可用: {}", e.getMessage());
        }
    }
}
