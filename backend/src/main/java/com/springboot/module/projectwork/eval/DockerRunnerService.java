package com.springboot.module.projectwork.eval;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.springboot.docker.DockerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.UUID;

/**
 * 负责：解压 ZIP → 创建 Docker 容器（挂载代码目录）→ 执行 npm install && npm run dev
 * → 等待端口就绪 → 返回可访问的 URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerRunnerService {

    private static final String NODE_IMAGE = "node:18-alpine";
    // 容器内部 Vite 默认端口
    private static final int CONTAINER_PORT = 5173;

    private final DockerManager dockerManager;
    private final ProjectworkEvalProperties props;

    /**
     * 解压 ZIP 到临时目录，返回解压后的绝对路径
     */
    public Path unzipProject(String zipFilePath) throws IOException {
        Path unzipRoot = Paths.get(props.getUnzipRoot());
        Files.createDirectories(unzipRoot);

        String taskDir = "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Path destDir = unzipRoot.resolve(taskDir);
        Files.createDirectories(destDir);

        try (ZipFile zipFile = new ZipFile(new File(zipFilePath))) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path entryPath = destDir.resolve(entry.getName()).normalize();
                // 安全检查：防止 zip slip 攻击
                if (!entryPath.startsWith(destDir)) {
                    throw new IOException("ZIP 解压路径越界: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        log.info("[DockerRunner] ZIP 解压完成: {} -> {}", zipFilePath, destDir);
        return destDir;
    }

    /**
     * 启动容器运行项目，返回宿主机可访问的端口号
     * @param projectDir 解压后的项目目录（绝对路径）
     * @return 容器 ID
     */
    public ContainerContext startContainer(Path projectDir) throws Exception {
        // 找一个可用的宿主机随机端口（容器内固定 5173）
        int hostPort = findFreePort();

        ExposedPort exposedPort = ExposedPort.tcp(CONTAINER_PORT);
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Ports.Binding.bindPort(hostPort));

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(projectDir.toAbsolutePath().toString(),
                        new Volume("/app")))
                .withPortBindings(portBindings)
                .withMemory(props.getContainerMemoryLimit())
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L)  // 0.5 core
                .withNetworkMode("bridge");

        DockerClient client = dockerManager.getClient();
        CreateContainerResponse container = client.createContainerCmd(NODE_IMAGE)
                .withWorkingDir("/app")
                .withCmd("sh", "-c", "npm install && npm run dev -- --host 0.0.0.0 --port " + CONTAINER_PORT)
                .withExposedPorts(exposedPort)
                .withHostConfig(hostConfig)
                .exec();

        String containerId = container.getId();
        dockerManager.startContainer(containerId);
        log.info("[DockerRunner] 容器启动: {} 映射端口: {}", containerId, hostPort);

        return new ContainerContext(containerId, hostPort, projectDir);
    }

    /**
     * 等待容器内 HTTP 服务就绪（轮询），超时抛异常
     */
    public void waitForReady(ContainerContext ctx) throws Exception {
        String url = "http://localhost:" + ctx.getHostPort();
        long deadline = System.currentTimeMillis() + props.getContainerStartupTimeout() * 1000L;

        while (System.currentTimeMillis() < deadline) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                if (code < 500) {
                    log.info("[DockerRunner] 服务就绪: {} HTTP {}", url, code);
                    return;
                }
            } catch (Exception ignored) {
                // 还未就绪，继续等待
            }
            Thread.sleep(3000);
        }
        throw new RuntimeException("容器服务启动超时（" + props.getContainerStartupTimeout() + "s），URL: " + url);
    }

    /**
     * 获取容器运行日志
     */
    public String getLog(String containerId) throws InterruptedException {
        return dockerManager.getContainerLog(containerId, 10);
    }

    /**
     * 停止并删除容器，清理解压目录
     */
    public void cleanup(ContainerContext ctx) {
        try {
            dockerManager.stopContainer(ctx.getContainerId(), 5);
        } catch (Exception e) {
            log.warn("[DockerRunner] 停止容器失败（忽略）: {}", e.getMessage());
        }
        try {
            dockerManager.removeContainer(ctx.getContainerId());
        } catch (Exception e) {
            log.warn("[DockerRunner] 删除容器失败（忽略）: {}", e.getMessage());
        }
        try {
            deleteDirectory(ctx.getProjectDir().toFile());
        } catch (Exception e) {
            log.warn("[DockerRunner] 清理目录失败（忽略）: {}", e.getMessage());
        }
    }

    // ---- 工具方法 ----

    private int findFreePort() throws IOException {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    // ---- 内部数据类 ----

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ContainerContext {
        private String containerId;
        private int hostPort;
        private Path projectDir;
    }
}
