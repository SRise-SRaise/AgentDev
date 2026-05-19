package com.springboot.module.projectwork.eval;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
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
 * Handles: unzip ZIP -> create Docker container (mount code dir) -> run npm install && npm run dev
 * -> wait for port ready -> return accessible URL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerRunnerService {

    private static final String NODE_IMAGE = "node:18-alpine";
    private static final int CONTAINER_PORT = 5173;

    private final DockerManager dockerManager;
    private final ProjectworkEvalProperties props;

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
                if (!entryPath.startsWith(destDir)) {
                    throw new IOException("Zip slip detected: " + entry.getName());
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
        log.info("[DockerRunner] Unzip done: {} -> {}", zipFilePath, destDir);
        return destDir;
    }

    public ContainerContext startContainer(Path projectDir) throws Exception {
        int hostPort = findFreePort();

        ExposedPort exposedPort = ExposedPort.tcp(CONTAINER_PORT);
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Ports.Binding.bindPort(hostPort));

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(projectDir.toAbsolutePath().toString(), new Volume("/app")))
                .withPortBindings(portBindings)
                .withMemory(props.getContainerMemoryLimit())
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L)
                .withNetworkMode("bridge");

        DockerClient client = dockerManager.getClient();
        CreateContainerResponse container = client.createContainerCmd(NODE_IMAGE)
                .withWorkingDir("/app")
                .withCmd("sh", "-c",
                        "npm install && npm run dev -- --host 0.0.0.0 --port " + CONTAINER_PORT)
                .withExposedPorts(exposedPort)
                .withHostConfig(hostConfig)
                .exec();

        String containerId = container.getId();
        dockerManager.startContainer(containerId);
        log.info("[DockerRunner] Container started: {} -> host port: {}", containerId, hostPort);

        return new ContainerContext(containerId, hostPort, projectDir);
    }

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
                    log.info("[DockerRunner] Service ready: {} HTTP {}", url, code);
                    return;
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(3000);
        }
        throw new RuntimeException("Container startup timeout ("
                + props.getContainerStartupTimeout() + "s), url: " + url);
    }

    public String getLog(String containerId) throws InterruptedException {
        return dockerManager.getContainerLog(containerId, 10);
    }

    public void cleanup(ContainerContext ctx) {
        try {
            dockerManager.stopContainer(ctx.getContainerId(), 5);
        } catch (Exception e) {
            log.warn("[DockerRunner] Stop container failed (ignored): {}", e.getMessage());
        }
        try {
            dockerManager.removeContainer(ctx.getContainerId());
        } catch (Exception e) {
            log.warn("[DockerRunner] Remove container failed (ignored): {}", e.getMessage());
        }
        try {
            deleteDirectory(ctx.getProjectDir().toFile());
        } catch (Exception e) {
            log.warn("[DockerRunner] Cleanup dir failed (ignored): {}", e.getMessage());
        }
    }

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

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ContainerContext {
        private String containerId;
        private int hostPort;
        private Path projectDir;
    }
}
