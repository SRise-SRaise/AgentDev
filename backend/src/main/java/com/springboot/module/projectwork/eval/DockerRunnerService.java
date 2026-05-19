package com.springboot.module.projectwork.eval;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.NetworkingConfig;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

/**
 * Manages Docker containers for project evaluation.
 *
 * FULLSTACK mode (default):
 *   ZIP structure:
 *     frontend/  — Vue/React (node:18-alpine, npm run dev)
 *     backend/   — Python    (python:3.11-slim, uvicorn / flask)
 *   Optional DB sidecar: if backend/ contains psycopg2/pymysql/asyncpg etc.
 *   a database container is auto-started on the shared network and its
 *   credentials are injected into the backend container as env vars.
 *
 * All three containers share the Docker network "eval_net":
 *   frontend → http://backend:8000
 *   backend  → DB_HOST=db, DB_PORT=3306|5432
 *
 * FRONTEND_ONLY mode:
 *   Single node:18-alpine container, original behaviour.
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

    // ---- DB detection ---------------------------------------------------------

    /**
     * Scans backend/ for common Python DB dependency keywords.
     * Returns the detected DbType, or NONE if no database is required.
     */
    public DbType detectDatabase(Path backendDir) {
        if (!Files.isDirectory(backendDir)) return DbType.NONE;

        // Files to scan for DB hints
        String[] filesToScan = {
            "requirements.txt", "requirements-dev.txt", "pyproject.toml",
            ".env.example", "config.py", "settings.py", "database.py",
            "db.py", "models.py", "app.py", "main.py"
        };

        for (String filename : filesToScan) {
            Path candidate = backendDir.resolve(filename);
            if (Files.exists(candidate)) {
                try {
                    String content = Files.readString(candidate).toLowerCase();
                    // PostgreSQL keywords
                    if (content.contains("psycopg2") || content.contains("asyncpg")
                            || content.contains("psycopg") || content.contains("postgresql")
                            || content.contains("postgres")) {
                        log.info("[DockerRunner] Detected PostgreSQL dependency in {}", filename);
                        return DbType.POSTGRESQL;
                    }
                    // MySQL / MariaDB keywords
                    if (content.contains("pymysql") || content.contains("mysqlclient")
                            || content.contains("aiomysql") || content.contains("mysql+")
                            || content.contains("mysql://")) {
                        log.info("[DockerRunner] Detected MySQL dependency in {}", filename);
                        return DbType.MYSQL;
                    }
                    // SQLite is handled by Python stdlib — no extra container needed
                    if (content.contains("sqlite")) {
                        log.info("[DockerRunner] Detected SQLite usage in {} — no sidecar needed", filename);
                        return DbType.SQLITE;
                    }
                } catch (IOException e) {
                    log.warn("[DockerRunner] Could not read {}: {}", candidate, e.getMessage());
                }
            }
        }
        log.info("[DockerRunner] No DB dependency detected in backend/");
        return DbType.NONE;
    }

    // ---- DB container ---------------------------------------------------------

    /**
     * Starts a database sidecar container on the shared network.
     * The container is given the hostname "db" so the backend can reach it
     * via DB_HOST=db regardless of which database engine is used.
     *
     * Returns null if dbType is NONE or SQLITE.
     */
    public ContainerContext startDbContainer(DbType dbType, String networkName) throws Exception {
        if (dbType == DbType.NONE || dbType == DbType.SQLITE) return null;

        int hostPort = findFreePort();
        boolean isPostgres = (dbType == DbType.POSTGRESQL);
        int containerPort = isPostgres ? 5432 : 3306;
        String image = isPostgres ? props.getPostgresImage() : props.getMysqlImage();

        ExposedPort exposed = ExposedPort.tcp(containerPort);
        Ports portBindings = new Ports();
        portBindings.bind(exposed, Ports.Binding.bindPort(hostPort));

        List<String> envVars = isPostgres
                ? List.of(
                    "POSTGRES_DB="       + props.getDbName(),
                    "POSTGRES_USER="     + props.getDbUser(),
                    "POSTGRES_PASSWORD=" + props.getDbPassword())
                : List.of(
                    "MYSQL_DATABASE="      + props.getDbName(),
                    "MYSQL_USER="          + props.getDbUser(),
                    "MYSQL_PASSWORD="      + props.getDbPassword(),
                    "MYSQL_ROOT_PASSWORD=" + props.getDbPassword());

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPortBindings(portBindings)
                .withMemory(256 * 1024 * 1024L)  // 256 MB — DB sidecar is lightweight
                .withNetworkMode(networkName);

        DockerClient client = dockerManager.getClient();
        CreateContainerResponse container = client.createContainerCmd(image)
                .withName("eval_db_" + hostPort)
                .withNetworkingConfig(buildNetworkingConfig(networkName, "db"))
                .withExposedPorts(exposed)
                .withEnv(envVars)
                .withHostConfig(hostConfig)
                .exec();

        String containerId = container.getId();
        dockerManager.startContainer(containerId);
        log.info("[DockerRunner] DB container ({}) started: {} -> host port: {}",
                dbType, containerId, hostPort);
        return new ContainerContext(containerId, hostPort, null, "db");
    }

    /**
     * Waits for the database port to accept TCP connections.
     * Uses raw socket connect instead of HTTP since DB ports are not HTTP.
     */
    public void waitForDbReady(ContainerContext dbCtx, int timeoutSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try (java.net.Socket s = new java.net.Socket()) {
                s.connect(new java.net.InetSocketAddress("localhost", dbCtx.getHostPort()), 2000);
                log.info("[DockerRunner] DB port {} is open", dbCtx.getHostPort());
                // Extra grace: give the DB engine 5s to finish initialization after port opens
                Thread.sleep(5000);
                return;
            } catch (Exception ignored) {
            }
            Thread.sleep(3000);
        }
        throw new RuntimeException("DB startup timeout (" + timeoutSeconds + "s) port=" + dbCtx.getHostPort());
    }

    // ---- Build backend env vars -----------------------------------------------

    /**
     * Returns the environment variable list to inject into the backend container,
     * encoding the DB connection parameters that the backend is expected to read
     * from os.environ (standard practice for both Flask and FastAPI projects).
     */
    private List<String> buildBackendEnv(DbType dbType) {
        List<String> env = new ArrayList<>();
        if (dbType == DbType.NONE || dbType == DbType.SQLITE) return env;

        boolean isPostgres = (dbType == DbType.POSTGRESQL);
        int dbPort = isPostgres ? 5432 : 3306;

        // Standard key names used by most Python DB projects
        env.add("DB_HOST=db");
        env.add("DB_PORT=" + dbPort);
        env.add("DB_NAME=" + props.getDbName());
        env.add("DB_USER=" + props.getDbUser());
        env.add("DB_PASSWORD=" + props.getDbPassword());
        env.add("DB_DATABASE=" + props.getDbName());  // alias used by some frameworks

        // SQLAlchemy / Tortoise-ORM style DATABASE_URL
        String urlScheme = isPostgres ? "postgresql+psycopg2" : "mysql+pymysql";
        String databaseUrl = String.format("%s://%s:%s@db:%d/%s",
                urlScheme, props.getDbUser(), props.getDbPassword(), dbPort, props.getDbName());
        env.add("DATABASE_URL=" + databaseUrl);

        // Async variants
        String asyncScheme = isPostgres ? "postgresql+asyncpg" : "mysql+aiomysql";
        String asyncUrl = String.format("%s://%s:%s@db:%d/%s",
                asyncScheme, props.getDbUser(), props.getDbPassword(), dbPort, props.getDbName());
        env.add("ASYNC_DATABASE_URL=" + asyncUrl);

        return env;
    }

    // ---- Container start ------------------------------------------------------

    /**
     * Entry point: starts all required containers for this submission.
     * Order: network -> db (if needed) -> backend -> frontend
     */
    public FullstackContext startContainers(Path projectDir) throws Exception {
        boolean isFullstack = "FULLSTACK".equalsIgnoreCase(props.getProjectType());

        if (!isFullstack) {
            return new FullstackContext(null, null, startFrontendContainer(projectDir));
        }

        Path frontendDir = projectDir.resolve("frontend");
        Path backendDir  = projectDir.resolve("backend");

        if (!Files.isDirectory(frontendDir) || !Files.isDirectory(backendDir)) {
            log.warn("[DockerRunner] FULLSTACK: frontend/ or backend/ missing, falling back to FRONTEND_ONLY");
            return new FullstackContext(null, null, startFrontendContainer(projectDir));
        }

        ensureNetwork(props.getSharedNetworkName());

        // Step A: detect DB dependency
        DbType dbType = props.isDbSidecarEnabled()
                ? detectDatabase(backendDir)
                : DbType.NONE;

        // Step B: start DB sidecar (if needed) and wait for it to accept TCP connections
        // BEFORE starting the backend — otherwise the backend will fail to connect on boot.
        ContainerContext dbCtx = startDbContainer(dbType, props.getSharedNetworkName());
        if (dbCtx != null) {
            log.info("[DockerRunner] Waiting for DB sidecar ({}) to be ready...", dbType);
            waitForDbReady(dbCtx, props.getDbStartupTimeout());
            log.info("[DockerRunner] DB sidecar ready, starting backend now");
        }

        // Step C: start backend (DB is now ready, env vars already injected)
        List<String> backendEnv = buildBackendEnv(dbType);
        ContainerContext backendCtx  = startBackendContainer(backendDir, backendEnv);

        // Step D: start frontend (backend is booting, will be waited on in Consumer)
        ContainerContext frontendCtx = startFrontendContainer(frontendDir);

        return new FullstackContext(dbCtx, backendCtx, frontendCtx);
    }

    private ContainerContext startBackendContainer(Path backendDir,
                                                   List<String> extraEnv) throws Exception {
        int hostPort = findFreePort();
        int containerPort = props.getBackendPort();
        String networkName = props.getSharedNetworkName();

        ExposedPort exposed = ExposedPort.tcp(containerPort);
        Ports portBindings = new Ports();
        portBindings.bind(exposed, Ports.Binding.bindPort(hostPort));

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(backendDir.toAbsolutePath().toString(), new Volume("/app")))
                .withPortBindings(portBindings)
                .withMemory(props.getContainerMemoryLimit())
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L)
                .withNetworkMode(networkName);

        DockerClient client = dockerManager.getClient();
        CreateContainerResponse container = client.createContainerCmd(props.getBackendImage())
                .withName("eval_backend_" + hostPort)
                .withNetworkingConfig(buildNetworkingConfig(networkName, "backend"))
                .withWorkingDir("/app")
                .withCmd("sh", "-c", props.getBackendStartCmd())
                .withEnv(extraEnv)
                .withExposedPorts(exposed)
                .withHostConfig(hostConfig)
                .exec();

        String containerId = container.getId();
        dockerManager.startContainer(containerId);
        log.info("[DockerRunner] Backend container started: {} hostPort={} dbEnvVars={} network={}",
                containerId, hostPort, extraEnv.size(), networkName);
        return new ContainerContext(containerId, hostPort, backendDir, "backend");
    }

    private ContainerContext startFrontendContainer(Path frontendDir) throws Exception {
        int hostPort = findFreePort();
        int containerPort = props.getFrontendPort();
        String networkName = props.getSharedNetworkName();

        ExposedPort exposed = ExposedPort.tcp(containerPort);
        Ports portBindings = new Ports();
        portBindings.bind(exposed, Ports.Binding.bindPort(hostPort));

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withBinds(new Bind(frontendDir.toAbsolutePath().toString(), new Volume("/app")))
                .withPortBindings(portBindings)
                .withMemory(props.getContainerMemoryLimit())
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L)
                .withNetworkMode(networkName);

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
        log.info("[DockerRunner] Frontend container started: {} hostPort={} network={}",
                containerId, hostPort, networkName);
        return new ContainerContext(containerId, hostPort, frontendDir, "frontend");
    }

    // ---- Networking helper ----------------------------------------------------

    /**
     * Builds a NetworkingConfig that registers the container under the given
     * network alias so other containers on the same bridge can resolve it by
     * the alias name (e.g. "db", "backend").
     * This replaces the deprecated .withHostname() on CreateContainerCmd.
     */
    private NetworkingConfig buildNetworkingConfig(String networkName, String alias) {
        ContainerNetwork containerNetwork = new ContainerNetwork()
                .withAliases(List.of(alias));
        NetworkingConfig networkingConfig = new NetworkingConfig();
        networkingConfig.setEndpointsConfig(
                java.util.Map.of(networkName, containerNetwork));
        return networkingConfig;
    }

    // ---- Wait for ready (HTTP) ------------------------------------------------

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
                    log.info("[DockerRunner] {} ready: {} HTTP {}", ctx.getRole(), url, code);
                    return;
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(3000);
        }
        throw new RuntimeException(ctx.getRole() + " startup timeout (" + timeoutSeconds + "s): " + url);
    }

    public void waitForReady(ContainerContext ctx) throws Exception {
        waitForReady(ctx, props.getContainerStartupTimeout());
    }

    // ---- Logs -----------------------------------------------------------------

    public String getLog(String containerId) throws InterruptedException {
        return dockerManager.getContainerLog(containerId, 10);
    }

    // ---- Cleanup --------------------------------------------------------------

    public void cleanup(FullstackContext ctx) {
        if (ctx.getFrontendCtx() != null) cleanupContainer(ctx.getFrontendCtx());
        if (ctx.getBackendCtx()  != null) cleanupContainer(ctx.getBackendCtx());
        if (ctx.getDbCtx()       != null) cleanupContainer(ctx.getDbCtx());

        // Remove unzipped project directory.
        // - FULLSTACK: frontendCtx.projectDir = {root}/task_xxx/frontend/  -> go up one level
        // - FRONTEND_ONLY: frontendCtx.projectDir = {root}/task_xxx/       -> use directly
        if (ctx.getFrontendCtx() != null && ctx.getFrontendCtx().getProjectDir() != null) {
            Path dir = ctx.getFrontendCtx().getProjectDir();
            boolean isSubdir = ctx.getBackendCtx() != null; // fullstack has a backend sibling
            deleteDirectory((isSubdir && dir.getParent() != null
                    ? dir.getParent() : dir).toFile());
        }

        // Remove shared network (best effort — it may not exist in FRONTEND_ONLY mode)
        try {
            dockerManager.getClient().removeNetworkCmd(props.getSharedNetworkName()).exec();
        } catch (Exception ignored) {}
    }

    private void cleanupContainer(ContainerContext ctx) {
        try { dockerManager.stopContainer(ctx.getContainerId(), 5); }
        catch (Exception e) { log.warn("[DockerRunner] stop [{}] failed: {}", ctx.getRole(), e.getMessage()); }
        try { dockerManager.removeContainer(ctx.getContainerId()); }
        catch (Exception e) { log.warn("[DockerRunner] remove [{}] failed: {}", ctx.getRole(), e.getMessage()); }
    }

    // ---- Docker network -------------------------------------------------------

    private String ensureNetwork(String networkName) {
        try {
            DockerClient client = dockerManager.getClient();
            try {
                client.inspectNetworkCmd().withNetworkId(networkName).exec();
            } catch (Exception notFound) {
                client.createNetworkCmd().withName(networkName).withDriver("bridge").exec();
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

    // ---- Enums and context classes -------------------------------------------

    public enum DbType {
        NONE,       // No database needed
        SQLITE,     // SQLite — handled by Python stdlib, no sidecar
        POSTGRESQL,
        MYSQL
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ContainerContext {
        private String containerId;
        private int hostPort;
        private Path projectDir;
        /** "frontend", "backend", or "db" */
        private String role;
    }

    /**
     * Holds all three ContainerContexts for a full-stack evaluation run.
     * dbCtx and backendCtx are null in FRONTEND_ONLY mode.
     * dbCtx is null when no database dependency is detected.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class FullstackContext {
        private ContainerContext dbCtx;
        private ContainerContext backendCtx;
        private ContainerContext frontendCtx;
    }
}
