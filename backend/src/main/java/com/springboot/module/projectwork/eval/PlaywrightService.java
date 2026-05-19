package com.springboot.module.projectwork.eval;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes screenshots of the running student project using Playwright Chromium,
 * and extracts page text as input for the AI evaluation prompt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightService {

    private final ProjectworkEvalProperties props;

    public ScreenshotResult capture(String baseUrl, Long taskId) throws IOException {
        Path screenshotDir = Paths.get(props.getScreenshotRoot()).resolve("task_" + taskId);
        Files.createDirectories(screenshotDir);

        List<String> screenshotPaths = new ArrayList<>();
        StringBuilder pageTextSummary = new StringBuilder();

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"));

            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions().setViewportSize(1280, 800));

                captureOnePage(context, baseUrl, screenshotDir,
                        "page_home.png", screenshotPaths, pageTextSummary);

                Thread.sleep(props.getScreenshotWaitMs());
                captureOnePage(context, baseUrl, screenshotDir,
                        "page_home_delayed.png", screenshotPaths, pageTextSummary);

                captureOnePage(context, baseUrl + "/login", screenshotDir,
                        "page_login.png", screenshotPaths, pageTextSummary);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Playwright] Screenshot wait interrupted");
        } catch (Exception e) {
            log.error("[Playwright] Screenshot error: {}", e.getMessage(), e);
        }

        log.info("[Playwright] Done, {} screenshots, taskId={}", screenshotPaths.size(), taskId);
        return new ScreenshotResult(screenshotPaths, pageTextSummary.toString().trim());
    }

    private void captureOnePage(BrowserContext context, String url, Path screenshotDir,
                                String fileName, List<String> paths, StringBuilder textSummary) {
        try (Page page = context.newPage()) {
            page.navigate(url, new Page.NavigateOptions().setTimeout(15000));
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(10000));

            Path screenshotPath = screenshotDir.resolve(fileName);
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setType(ScreenshotType.PNG)
                    .setFullPage(false));
            paths.add(screenshotPath.toAbsolutePath().toString());

            String title = page.title();
            String bodyText = page.innerText("body");
            if (bodyText.length() > 500) bodyText = bodyText.substring(0, 500) + "...";
            textSummary.append("[").append(fileName).append("]\n")
                    .append("title: ").append(title).append("\n")
                    .append("body: ").append(bodyText).append("\n\n");

        } catch (Exception e) {
            log.warn("[Playwright] Failed to capture {}: {}", url, e.getMessage());
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ScreenshotResult {
        private List<String> screenshotPaths;
        private String pageTextSummary;
    }
}
