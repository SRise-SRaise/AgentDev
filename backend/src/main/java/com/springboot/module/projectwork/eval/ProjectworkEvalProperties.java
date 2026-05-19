package com.springboot.module.projectwork.eval;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "projectwork.eval")
public class ProjectworkEvalProperties {

    /** Max seconds to wait for container HTTP port to be ready */
    private int containerStartupTimeout = 120;

    /** Overall task timeout in seconds */
    private int taskTimeout = 180;

    /** Milliseconds to wait before taking screenshots */
    private long screenshotWaitMs = 3000;

    /** Container memory limit in bytes (default 512MB) */
    private long containerMemoryLimit = 536870912L;

    /** Root directory for unzipped project files */
    private String unzipRoot = "storage/project_unzip";

    /** Root directory for screenshot files */
    private String screenshotRoot = "storage/screenshots";

    // ---- Full-stack project settings ----

    /**
     * Project type: FRONTEND_ONLY or FULLSTACK.
     * FULLSTACK expects frontend/ and backend/ subdirectories in the ZIP.
     */
    private String projectType = "FULLSTACK";

    /** Docker image for the frontend container */
    private String frontendImage = "node:18-alpine";

    /** Port exposed by the frontend container (Vite default) */
    private int frontendPort = 5173;

    /** Shell command to start the frontend inside /app */
    private String frontendStartCmd = "npm install && npm run dev -- --host 0.0.0.0 --port 5173";

    /** Docker image for the backend container */
    private String backendImage = "python:3.11-slim";

    /** Port exposed by the backend container (uvicorn/Flask default) */
    private int backendPort = 8000;

    /** Shell command to start the backend inside /app */
    private String backendStartCmd =
            "pip install -r requirements.txt -q && uvicorn main:app --host 0.0.0.0 --port 8000";

    /** Max seconds to wait for backend container to be ready (separate from frontend) */
    private int backendStartupTimeout = 90;

    /** Docker network name shared between frontend and backend containers */
    private String sharedNetworkName = "eval_net";
}
