package com.springboot.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DockerManager {

    private final DockerClient dockerClient;

    private final DockerHttpClient dockerHttpClient;

    public DockerManager(DockerProperties properties) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(properties.getHost())
                .withDockerTlsVerify(properties.isTlsVerify())
                .withDockerCertPath(properties.getCertPath())
                .build();

        this.dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(properties.getMaxConnections())
                .connectionTimeout(Duration.ofSeconds(properties.getConnectionTimeoutSeconds()))
                .responseTimeout(Duration.ofSeconds(properties.getResponseTimeoutSeconds()))
                .build();

        this.dockerClient = DockerClientImpl.getInstance(config, dockerHttpClient);
        log.info("DockerClient 初始化完成，连接: {}", properties.getHost());
    }

    @PreDestroy
    public void close() {
        try {
            dockerHttpClient.close();
            log.info("DockerClient 连接已关闭");
        } catch (IOException e) {
            log.error("关闭 DockerClient 连接失败", e);
        }
    }

    public DockerClient getClient() {
        return dockerClient;
    }

    public String createContainer(String image, String... cmd) {
        CreateContainerResponse response = dockerClient.createContainerCmd(image)
                .withCmd(cmd)
                .exec();
        String containerId = response.getId();
        log.info("创建容器成功: {}", containerId);
        return containerId;
    }

    public String createContainerWithResourceLimits(String image, long memoryLimitBytes, long cpuPeriod, long cpuQuota, String... cmd) {
        CreateContainerResponse response = dockerClient.createContainerCmd(image)
                .withCmd(cmd)
                .withHostConfig(HostConfig.newHostConfig()
                        .withMemory(memoryLimitBytes)
                        .withCpuPeriod(cpuPeriod)
                        .withCpuQuota(cpuQuota)
                        .withNetworkMode("none"))
                .exec();
        String containerId = response.getId();
        log.info("创建受限容器成功: {}, 内存限制: {}MB, CPU: {}/{}", containerId, memoryLimitBytes / 1024 / 1024, cpuQuota, cpuPeriod);
        return containerId;
    }

    public void startContainer(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
        log.info("启动容器: {}", containerId);
    }

    public void stopContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
        log.info("停止容器: {}", containerId);
    }

    public void stopContainer(String containerId, int timeoutSeconds) {
        dockerClient.stopContainerCmd(containerId).withTimeout(timeoutSeconds).exec();
        log.info("停止容器: {}, 超时: {}s", containerId, timeoutSeconds);
    }

    public void removeContainer(String containerId) {
        dockerClient.removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).exec();
        log.info("移除容器: {}", containerId);
    }

    public String getContainerLog(String containerId) throws InterruptedException {
        StringBuilder logBuilder = new StringBuilder();
        LogContainerCmd logCmd = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withTailAll();

        try (ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame frame) {
                logBuilder.append(new String(frame.getPayload()));
            }
        }) {
            logCmd.exec(callback);
            callback.awaitCompletion();
        } catch (IOException e) {
            log.error("获取容器日志失败: {}", containerId, e);
        }

        return logBuilder.toString();
    }

    public String getContainerLog(String containerId, long timeoutSeconds) throws InterruptedException {
        StringBuilder logBuilder = new StringBuilder();
        LogContainerCmd logCmd = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withTailAll();

        try (ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame frame) {
                logBuilder.append(new String(frame.getPayload()));
            }
        }) {
            logCmd.exec(callback);
            callback.awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);
        } catch (IOException e) {
            log.error("获取容器日志失败: {}", containerId, e);
        }

        return logBuilder.toString();
    }

    public String executeCommand(String containerId, String... cmd) throws InterruptedException {
        ExecCreateCmdResponse execCreate = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(cmd)
                .exec();

        StringBuilder output = new StringBuilder();
        try (ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<Frame>() {
            @Override
            public void onNext(Frame frame) {
                output.append(new String(frame.getPayload()));
            }
        }) {
            dockerClient.execStartCmd(execCreate.getId()).exec(callback);
            callback.awaitCompletion();
        } catch (IOException e) {
            log.error("执行命令失败: containerId={}, cmd={}", containerId, String.join(" ", cmd), e);
        }

        return output.toString();
    }

    public boolean isContainerRunning(String containerId) {
        try {
            return dockerClient.inspectContainerCmd(containerId).exec().getState().getRunning();
        } catch (Exception e) {
            log.error("检查容器状态失败: {}", containerId, e);
            return false;
        }
    }

    public void pullImage(String image) throws InterruptedException {
        try (ResultCallback.Adapter<PullResponseItem> callback = new ResultCallback.Adapter<>()) {
            dockerClient.pullImageCmd(image).exec(callback);
            callback.awaitCompletion();
            log.info("拉取镜像完成: {}", image);
        } catch (IOException e) {
            log.error("拉取镜像失败: {}", image, e);
        }
    }

    public List<String> listContainers() {
        return dockerClient.listContainersCmd().exec()
                .stream()
                .map(c -> c.getId() + " - " + c.getStatus() + " - " + String.join(", ", c.getNames()))
                .toList();
    }
}
