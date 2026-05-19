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
 * Manages Docker containers for project evaluation.
 *
 * FULLSTACK mode (default):
 *   ZIP must contain:
 *     frontend/  — Vue/React project, started with npm run dev
 *     backend/   — Python project, started with uvicorn/flask
 *   Two containers are created on a shared Docker network so that
 *   the frontend can reach the backend via http://backend:8000.
 *
 * FRONTEND_ONLY mode:
 *   Original behaviour — single node:18-alpine container.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerRunnerService {

    private final DockerManager dockerManager;
    private final ProjectworkEvalProperties props;

    // ---- Unzip ----------------------------------------------------------------

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

    // ---- Container start ------------------------------------------------------

    /**
     * Starts container(s) based on projectType.
     * Returns a FullstackContext that holds both backend and frontend ContainerContexts
     * (backendCtx is null in FRONTEND_ONLY mode).
     */
    public FullstackContext startContainers(Path projectDir) throws Exception {
        boolean isFullstack = "FULLSTACK".equalsIgnoreCase(props.getProjectType());

        if (isFullstack) {
            Path frontendDir = projectDir.resolve("frontend");
            Path backendDir  = projectDir.resolve("backend");

            if (!Files.isDirectory(frontendDir) || !Files.isDirectory(backendDir)) {
                log.warn("[DockerRunner] FULLSTACK mode but frontend/ or backend/ not found, "
                        + "falling back to FRONTEND_ONLY");
                return new FullstackContext(null, startFrontendContainer(projectDir, null));
            }

            // Create a shared network so frontend container can reach backend by hostname
            String networkId = ensureNetwork(props.getSharedNetworkName());

            ContainerContext backendCtx  = startBackendContainer(backendDir, networkId);
            ContainerContext frontendCtx = startFrontendContainer(frontendDir, networkId);
            return new FullstackContext(backendCtx, frontendCtx);
        } else {
            return new FullstackContext(null, startFrontendContainer(projectDir, null));
        }
    }

    private ContainerContext startBackendContainer(Path backendDir, String networkId) throws Exception {
        int hostPort = findFreePort();
        int containerPort = props.getBackendPort();

        ExposedPort exposed = ExposedPort.tcp(containerPort);
        Ports portBindings = new Ports();
        portBindings.bind(exposed, Ports.Binding.bindPort(hostPort));

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(backendDir.toAbsolutePath().toString(), new Volume("/app")))
                .withPortBindings(portBindings)
                .withMemory(props.getContainerMemoryLimit())
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L)
                .withNetworkMode(networkId != null ? props.getSharedNetworkName() : "bridge");

        DockerClient client = dockerManager.getClient();
        CreateContainerResponse container = client.createContainerCmd(props.getBackendImage())
                .withName("eval_backend_" + hostPort)
                .withHostname("backend")   // frontend reaches it via http://backend:8000
                .withWorkingDir("/app")
                .withCmd("sh", "-c", props.getBackendStartCmd())
                .withExposedPorts(exposed)
                .withHostConfig(hostConfig)
                .exec();

        String containerId = container.getId();
        dockerManager.startContainer(containerId);
        log.info("[DockerRunner] Backend container started: {} -> host port: {}", containerId, hostPort);
        return new ContainerContext(containerId, hostPort, backendDir, "backend");
    }

    private ContainerContext startFrontendContainer(Path frontendDir, String networkId) throws Exception {
        int hostPort = findFreePort();
        int containerPort = props.getFrontendPort();

        ExposedPort exposed = ExposedPort.tcp(containerPort);
        Ports portBindings = new Ports();
        portBindings.bind(exposed, Ports.Binding.bindPort(hostPort));

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(frontendDir.toAbsolutePath().toString(), new Volume("/app")))
                .withPortBindings(portBindings)
                .withMemory(props.getContainerMemoryLimit())
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L)
                .withNetworkMode(networkId != null ? props.getSharedNetworkName() : "bridge");

        DockerClient client = dockerManager.getClient();
        CreateContainerResponse container = client.createContainerCmd(props.getFrontendImage())
                .withName("eval_frontend_" + hostPort)
                .withWorkingDir("/app")
                .withCmd("sh", "-c", props.getFrontendStartCmd())
                .withExposedPorts(exposed)
                .withHostConfig(hostConfig)
                .exec();

        String containerId = container.getId();
        dockerManager.startContainer(containerId);
        log.info("[DockerRunner] Frontend container started: {} -> host port: {}", containerId, hostPort);
        return new ContainerContext(containerId, hostPort, frontendDir, "frontend");
    }

    // ---- Wait for ready -------------------------------------------------------

    public void waitForReady(ContainerContext ctx, int timeoutSeconds) throws Exception {
        String url = "http://localhost:" + ctx.getHostPort();
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

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
        throw new RuntimeException("Container startup timeout (" + timeoutSeconds + "s), url: " + url);
    }

    /** Convenience overload using frontend timeout from properties */
    public void waitForReady(ContainerContext ctx) throws Exception {
        waitForReady(ctx, props.getContainerStartupTimeout());
    }

    // ---- Logs -----------------------------------------------------------------

    public String getLog(String containerId) throws InterruptedException {
        return dockerManager.getContainerLog(containerId, 10);
    }

    // ---- Cleanup --------------------------------------------------------------

    public void cleanup(FullstackContext ctx) {
        if (ctx.getBackendCtx() != null) {
            cleanupContainer(ctx.getBackendCtx());
        }
        cleanupContainer(ctx.getFrontendCtx());
        // Remove project dir (parent of frontend/ backend/)
        Path projectDir = ctx.getFrontendCtx().getProjectDir().getParent();
        if (projectDir != null) {
            deleteDirectory(projectDir.toFile());
        } else {
            deleteDirectory(ctx.getFrontendCtx().getProjectDir().toFile());
        }
        // Remove shared network (best effort)
        try {
            dockerManager.getClient().removeNetworkCmd(props.getSharedNetworkName()).exec();
        } catch (Exception ignored) {}
    }

    private void cleanupContainer(ContainerContext ctx) {
        try { dockerManager.stopContainer(ctx.getContainerId(), 5); }
        catch (Exception e) { log.warn("[DockerRunner] Stop {} failed: {}", ctx.getRole(), e.getMessage()); }
        try { dockerManager.removeContainer(ctx.getContainerId()); }
        catch (Exception e) { log.warn("[DockerRunner] Remove {} failed: {}", ctx.getRole(), e.getMessage()); }
    }

    // ---- Docker network -------------------------------------------------------

    private String ensureNetwork(String networkName) {
        try {
            DockerClient client = dockerManager.getClient();
            // Try to inspect; if not found, create it
            try {
                client.inspectNetworkCmd().withNetworkId(networkName).exec();
                log.debug("[DockerRunner] Network '{}' already exists", networkName);
            } catch (Exception notFound) {
                client.createNetworkCmd()
                        .withName(networkName)
                        .withDriver("bridge")
                        .exec();
                log.info("[DockerRunner] Created Docker network '{}'", networkName);
            }
            return networkName;
        } catch (Exception e) {
            log.warn("[DockerRunner] Could not ensure network '{}': {}", networkName, e.getMessage());
            return null;
        }
    }

    // ---- Utilities ------------------------------------------------------------

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

    // ---- Context classes ------------------------------------------------------

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ContainerContext {
        private String containerId;
        private int hostPort;
        private Path projectDir;
        /** "frontend" or "backend" — used for logging */
        private String role;
    }

    /**
     * Holds both backend and frontend ContainerContexts.
     * backendCtx is null in FRONTEND_ONLY mode.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class FullstackContext {
        private ContainerContext backendCtx;
        private ContainerContext frontendCtx;
    }
}
