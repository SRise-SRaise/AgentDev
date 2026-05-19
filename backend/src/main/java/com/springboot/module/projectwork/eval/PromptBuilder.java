package com.springboot.module.projectwork.eval;

import com.springboot.model.entity.projectwork.ProjectAssignment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Builds prompts for GLM evaluation calls.
 * System prompt is fixed; user prompt is dynamically assembled per evaluation task.
 */
@Component
public class PromptBuilder {

    public static final String SYSTEM_PROMPT =
            "You are a professional teaching assistant for a full-stack Web development course. "
            + "Your job is to objectively evaluate student full-stack projects (Vue frontend + Python backend) "
            + "according to the given rubric. "
            + "Score each dimension independently with specific deduction reasons. "
            + "Return ONLY a JSON object - no markdown fences, no extra text.";

    public static final String RESPONSE_FORMAT =
            "{\n"
            + "  \"summary\": \"overall evaluation within 100 characters\",\n"
            + "  \"advantage\": \"main strengths, newline-separated\",\n"
            + "  \"problem\": \"main issues, newline-separated\",\n"
            + "  \"suggestion\": \"improvement suggestions\",\n"
            + "  \"dimensions\": [\n"
            + "    {\"name\": \"dimension name\", \"score\": 35, \"total\": 40, \"reason\": \"deduction reason\"}\n"
            + "  ],\n"
            + "  \"agent_score\": 85\n"
            + "}";

    /**
     * Builds a log-summary prompt for a full-stack project.
     * All three log sources are optional (null / blank = "(none)").
     * dbLog is typically the PostgreSQL/MySQL startup output.
     */
    public String buildLogSummaryPrompt(String frontendLog, String backendLog, String dbLog) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summarize the following project run logs in no more than 400 characters total. ")
          .append("Focus on errors, warnings, startup status, DB connection results, ")
          .append("and whether the backend API started successfully.\n\n");
        sb.append("[FRONTEND LOG (last 100 lines)]\n").append(trimLog(frontendLog, 100)).append("\n\n");
        sb.append("[BACKEND LOG (last 100 lines)]\n").append(trimLog(backendLog, 100));
        if (dbLog != null && !dbLog.isBlank()) {
            sb.append("\n\n[DB LOG (last 50 lines)]\n").append(trimLog(dbLog, 50));
        }
        return sb.toString();
    }

    /** Two-arg overload for callers without a DB sidecar */
    public String buildLogSummaryPrompt(String frontendLog, String backendLog) {
        return buildLogSummaryPrompt(frontendLog, backendLog, "");
    }

    /** Backwards-compatible single-arg overload for FRONTEND_ONLY callers */
    public String buildLogSummaryPrompt(String rawLog) {
        return buildLogSummaryPrompt(rawLog, "", "");
    }

    private String trimLog(String log, int maxLines) {
        if (log == null || log.isBlank()) return "(none)";
        String[] lines = log.split("\n");
        int start = Math.max(0, lines.length - maxLines);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    public String buildEvalPrompt(
            ProjectAssignment assignment,
            String logSummary,
            String pageTextSummary,
            String fileTree) {

        return "[Assignment Title]\n" + assignment.getTitle() + "\n\n"
                + "[Requirements]\n" + nvl(assignment.getRequirement()) + "\n\n"
                + "[Rubric (total 100 pts)]\n" + nvl(assignment.getRubricJson()) + "\n\n"
                + "[Project File Structure (frontend/ + backend/, 2 levels)]\n" + nvl(fileTree) + "\n\n"
                + "[Run Log Summary (frontend + backend + db)]\n" + nvl(logSummary) + "\n\n"
                + "[Page Snapshot + Backend API Probe]\n" + nvl(pageTextSummary) + "\n\n"
                + "[Response Format (strict JSON, no markdown)]\n" + RESPONSE_FORMAT;
    }

    /**
     * Builds a file-tree string for the AI prompt.
     *
     * For FULLSTACK projects the ZIP contains frontend/ and backend/ subdirectories.
     * We scan each up to 2 levels deep and label them separately so the AI can see
     * the structure of both layers.  Falls back to a single-root scan when neither
     * subdirectory exists (FRONTEND_ONLY / legacy submissions).
     */
    public String buildFileTree(Path projectDir) {
        StringBuilder sb = new StringBuilder();
        Path frontendDir = projectDir.resolve("frontend");
        Path backendDir  = projectDir.resolve("backend");

        boolean hasFrontend = Files.isDirectory(frontendDir);
        boolean hasBackend  = Files.isDirectory(backendDir);

        if (hasFrontend || hasBackend) {
            if (hasFrontend) {
                sb.append("[frontend/]\n");
                buildTreeRecursive(frontendDir, sb, 1, 2);
            }
            if (hasBackend) {
                sb.append("[backend/]\n");
                buildTreeRecursive(backendDir, sb, 1, 2);
            }
        } else {
            // Fallback: legacy single-root or frontend-only ZIP
            Path srcDir = projectDir.resolve("src");
            if (!Files.exists(srcDir)) srcDir = projectDir;
            buildTreeRecursive(srcDir, sb, 0, 2);
        }
        return sb.toString();
    }

    private void buildTreeRecursive(Path dir, StringBuilder sb, int depth, int maxDepth) {
        if (depth > maxDepth) return;
        try (Stream<Path> children = Files.list(dir)) {
            List<Path> sorted = new ArrayList<>();
            children.forEach(sorted::add);
            sorted.sort(java.util.Comparator.comparing(p -> p.getFileName().toString()));
            for (Path child : sorted) {
                String indent = "  ".repeat(depth);
                String name = child.getFileName().toString();
                if (name.startsWith(".") || name.equals("node_modules")) continue;
                if (Files.isDirectory(child)) {
                    sb.append(indent).append(name).append("/\n");
                    buildTreeRecursive(child, sb, depth + 1, maxDepth);
                } else {
                    sb.append(indent).append(name).append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("(failed to read dir)");
        }
    }

    private String nvl(String s) {
        return s == null ? "(none)" : s;
    }
}
