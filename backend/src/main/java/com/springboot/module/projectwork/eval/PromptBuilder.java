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
            "You are a professional teaching assistant for a Web Frontend course. "
            + "Your job is to objectively evaluate student projects according to the given rubric. "
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

    public String buildLogSummaryPrompt(String rawLog) {
        String[] lines = rawLog.split("\n");
        int start = Math.max(0, lines.length - 200);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return "Summarize the following Node.js project run log in no more than 200 characters. "
                + "Focus on errors, warnings and startup status.\n\n"
                + "[LOG]\n" + sb;
    }

    public String buildEvalPrompt(
            ProjectAssignment assignment,
            String logSummary,
            String pageTextSummary,
            String fileTree) {

        return "[Assignment Title]\n" + assignment.getTitle() + "\n\n"
                + "[Requirements]\n" + nvl(assignment.getRequirement()) + "\n\n"
                + "[Rubric (total 100 pts)]\n" + nvl(assignment.getRubricJson()) + "\n\n"
                + "[File Structure (src/ 2 levels)]\n" + nvl(fileTree) + "\n\n"
                + "[Run Log Summary]\n" + nvl(logSummary) + "\n\n"
                + "[Page Snapshot]\n" + nvl(pageTextSummary) + "\n\n"
                + "[Response Format (strict JSON, no markdown)]\n" + RESPONSE_FORMAT;
    }

    public String buildFileTree(Path projectDir) {
        StringBuilder sb = new StringBuilder();
        Path srcDir = projectDir.resolve("src");
        if (!Files.exists(srcDir)) {
            srcDir = projectDir;
        }
        buildTreeRecursive(srcDir, sb, 0, 2);
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
