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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures screenshots of the running student project using Playwright Chromium.
 *
 * For FULLSTACK projects an additional API connectivity check is performed:
 *   - Sends a GET to http://localhost:{backendPort}/  (or /api/health if present)
 *     and records the HTTP status code as evidence of backend liveness.
 *   - Captures screenshots of the frontend pages that exercise backend data
 *     (home with data loaded, login, and one data-heavy page after waiting).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightService {

    private final ProjectworkEvalProperties props;

    // ---- Public API ----------------------------------------------------------

    /**
     * Capture screenshots for a FULLSTACK project.
     *
     * @param frontendUrl  http://localhost:{frontendHostPort}
     * @param backendUrl   http://localhost:{backendHostPort} — may be null (FRONTEND_ONLY)
     * @param taskId       used to create an isolated screenshot directory
     */
    public ScreenshotResult capture(String frontendUrl, String backendUrl, Long taskId) throws IOException {
        Path screenshotDir = Paths.get(props.getScreenshotRoot()).resolve("task_" + taskId);
        Files.createDirectories(screenshotDir);

        List<String> screenshotPaths = new ArrayList<>();
        StringBuilder pageTextSummary = new StringBuilder();

        // 1. Backend API health check (before opening the browser)
        if (backendUrl != null) {
            String apiStatus = probeBackendApi(backendUrl);
            pageTextSummary.append("[Backend API Probe]\n").append(apiStatus).append("\n\n");
        }

        // 2. Frontend screenshots
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"));

            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions().setViewportSize(1280, 800));

                // Home page — immediate
                captureOnePage(context, frontendUrl, screenshotDir,
                        "page_home.png", screenshotPaths, pageTextSummary);

                // Home page — after waiting for async data to load
                Thread.sleep(props.getScreenshotWaitMs());
                captureOnePage(context, frontendUrl, screenshotDir,
                        "page_home_loaded.png", screenshotPaths, pageTextSummary);

                // Login page
                captureOnePage(context, frontendUrl + "/login", screenshotDir,
                        "page_login.png", screenshotPaths, pageTextSummary);

                // For fullstack: try a typical data page (/dashboard or /home)
                if (backendUrl != null) {
                    captureOnePage(context, frontendUrl + "/dashboard", screenshotDir,
                            "page_dashboard.png", screenshotPaths, pageTextSummary);
                }
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

    /**
     * Backwards-compatible overload for FRONTEND_ONLY callers.
     */
    public ScreenshotResult capture(String frontendUrl, Long taskId) throws IOException {
        return capture(frontendUrl, null, taskId);
    }

    // ---- Backend API probe ---------------------------------------------------

    /**
     * Probes the backend API root and a set of common health-check paths.
     * Returns a short text summary of HTTP status codes found, e.g.:
     *   GET /           -> 200 OK
     *   GET /api/health -> 200 OK
     *   GET /docs       -> 404 Not Found
     */
    private String probeBackendApi(String backendUrl) {
        String[] paths = {"/", "/api/health", "/health", "/docs", "/api"};
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            String fullUrl = backendUrl + path;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                sb.append("GET ").append(path).append(" -> ").append(code).append("\n");
            } catch (Exception e) {
                sb.append("GET ").append(path).append(" -> ERROR: ").append(e.getMessage()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    // ---- Page capture -------------------------------------------------------

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

    // ---- Result type --------------------------------------------------------

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ScreenshotResult {
        private List<String> screenshotPaths;
        private String pageTextSummary;
    }
}
