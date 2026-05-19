package com.springboot.module.projectwork.eval;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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
import java.util.UUID;

/**
 * 使用 Playwright Chromium 对运行中的学生项目进行截图，
 * 同时提取页面文本摘要（作为 AI 的文本输入之一）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightService {

    private final ProjectworkEvalProperties props;

    /**
     * 对指定 URL 进行截图，返回截图文件的绝对路径列表
     * @param baseUrl  容器暴露的 HTTP 地址，如 http://localhost:34521
     * @param taskId   任务 ID，用于区分截图目录
     */
    public ScreenshotResult capture(String baseUrl, Long taskId) throws IOException {
        Path screenshotDir = Paths.get(props.getScreenshotRoot()).resolve("task_" + taskId);
        Files.createDirectories(screenshotDir);

        List<String> screenshotPaths = new ArrayList<>();
        StringBuilder pageTextSummary = new StringBuilder();

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of(
                            "--no-sandbox",
                            "--disable-dev-shm-usage",
                            "--disable-gpu"
                    ));

            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions().setViewportSize(1280, 800));

                // 抓取首页
                captureOnePage(context, baseUrl, screenshotDir, "page_home.png",
                        screenshotPaths, pageTextSummary);

                // 等待一段时间后再截一张（捕获可能有延迟渲染的内容）
                Thread.sleep(props.getScreenshotWaitMs());
                captureOnePage(context, baseUrl, screenshotDir, "page_home_delayed.png",
                        screenshotPaths, pageTextSummary);

                // 尝试抓取 /login 路由（常见学生项目页面）
                captureOnePage(context, baseUrl + "/login", screenshotDir, "page_login.png",
                        screenshotPaths, pageTextSummary);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Playwright] 截图等待中断");
        } catch (Exception e) {
            log.error("[Playwright] 截图异常: {}", e.getMessage(), e);
        }

        log.info("[Playwright] 截图完成，共 {} 张，taskId={}", screenshotPaths.size(), taskId);
        return new ScreenshotResult(screenshotPaths, pageTextSummary.toString().trim());
    }

    private void captureOnePage(BrowserContext context, String url, Path screenshotDir,
                                  String fileName, List<String> paths, StringBuilder textSummary) {
        try (Page page = context.newPage()) {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(15000));
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(10000));

            // 截图
            Path screenshotPath = screenshotDir.resolve(fileName);
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setType(ScreenshotType.PNG)
                    .setFullPage(false));
            paths.add(screenshotPath.toAbsolutePath().toString());

            // 提取页面文本（截断至 500 字，避免 token 过多）
            String title = page.title();
            String bodyText = page.innerText("body");
            if (bodyText.length() > 500) bodyText = bodyText.substring(0, 500) + "...";
            textSummary.append("[页面 ").append(fileName).append("]\n")
                    .append("title: ").append(title).append("\n")
                    .append("正文摘要: ").append(bodyText).append("\n\n");

        } catch (Exception e) {
            log.warn("[Playwright] 抓取页面失败 {}: {}", url, e.getMessage());
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ScreenshotResult {
        private List<String> screenshotPaths;
        private String pageTextSummary;
    }
}
