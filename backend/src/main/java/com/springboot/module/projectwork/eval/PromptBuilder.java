package com.springboot.module.projectwork.eval;

import com.springboot.model.entity.projectwork.ProjectAssignment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 构建发送给 GLM 的评测 Prompt。
 * System Prompt 固定，User Prompt 每次动态注入评分标准、日志、页面文本、文件结构。
 */
@Component
public class PromptBuilder {

    public static final String SYSTEM_PROMPT =
            "你是一位 Web 前端课程的专业助教，负责对学生大作业进行客观、公正的自动化评测。\n" +
            "评分时请严格按照给定的评分标准，每个维度独立评分，给出具体扣分依据。\n" +
            "你必须以 JSON 格式返回结果，不得包含任何 JSON 以外的内容（不要加 markdown 代码块标记）。";

    public static final String RESPONSE_FORMAT =
            "{\n" +
            "  \"summary\": \"100字以内的总体评价\",\n" +
            "  \"advantage\": \"主要优点，用换行分隔\",\n" +
            "  \"problem\": \"主要问题，用换行分隔\",\n" +
            "  \"suggestion\": \"改进建议\",\n" +
            "  \"dimensions\": [\n" +
            "    {\"name\": \"维度名\", \"score\": 35, \"total\": 40, \"reason\": \"具体扣分原因\"}\n" +
            "  ],\n" +
            "  \"agent_score\": 85\n" +
            "}";

    /**
     * 构建日志摘要 Prompt（给 fastModel 压缩日志用）
     */
    public String buildLogSummaryPrompt(String rawLog) {
        // 只取最后 200 行
        String[] lines = rawLog.split("\n");
        int start = Math.max(0, lines.length - 200);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return "请将以下 Node.js 项目运行日志压缩为不超过 200 字的摘要，重点关注错误信息、警告和启动状态。\n\n"
                + "【日志内容】\n" + sb;
    }

    /**
     * 构建主评测 User Prompt
     */
    public String buildEvalPrompt(
            ProjectAssignment assignment,
            String logSummary,
            String pageTextSummary,
            String fileTree) {

        return "【大作业题目】\n" + assignment.getTitle() + "\n\n"
                + "【项目要求】\n" + nvl(assignment.getRequirement()) + "\n\n"
                + "【评分标准（总分100分）】\n" + nvl(assignment.getRubricJson()) + "\n\n"
                + "【文件结构（src/ 下两层）】\n" + nvl(fileTree) + "\n\n"
                + "【运行日志摘要】\n" + nvl(logSummary) + "\n\n"
                + "【页面访问情况】\n" + nvl(pageTextSummary) + "\n\n"
                + "【返回格式（严格 JSON，不加任何 markdown 标记）】\n" + RESPONSE_FORMAT;
    }

    /**
     * 生成项目文件树（src/ 下两层，避免 token 过多）
     */
    public String buildFileTree(Path projectDir) {
        StringBuilder sb = new StringBuilder();
        Path srcDir = projectDir.resolve("src");
        if (!Files.exists(srcDir)) {
            srcDir = projectDir; // 没有 src 目录就从根目录开始
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
            sb.append("(读取目录失败)");
        }
    }

    private String nvl(String s) {
        return s == null ? "(无)" : s;
    }
}
