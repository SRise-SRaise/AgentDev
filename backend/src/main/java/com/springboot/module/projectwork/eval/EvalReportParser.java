package com.springboot.module.projectwork.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 解析 GLM 返回的 JSON 字符串，提取各字段写入 AgentEvalReport。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvalReportParser {

    private final ObjectMapper objectMapper;

    /**
     * 从 LLM 原始返回文本中解析结构化结果
     */
    public ParsedReport parse(String llmResponse) {
        String json = extractJson(llmResponse);
        try {
            JsonNode root = objectMapper.readTree(json);
            ParsedReport report = new ParsedReport();
            report.setSummary(getText(root, "summary"));
            report.setAdvantage(getText(root, "advantage"));
            report.setProblem(getText(root, "problem"));
            report.setSuggestion(getText(root, "suggestion"));
            report.setRawJson(json);

            JsonNode scoreNode = root.get("agent_score");
            if (scoreNode != null && scoreNode.isNumber()) {
                report.setAgentScore(BigDecimal.valueOf(scoreNode.doubleValue())
                        .min(BigDecimal.valueOf(100))
                        .max(BigDecimal.ZERO));
            } else {
                // 尝试从 dimensions 汇总
                report.setAgentScore(sumDimensions(root));
            }
            return report;
        } catch (Exception e) {
            log.error("[EvalReportParser] JSON 解析失败，原始内容: {}", llmResponse, e);
            // 降级：返回原始文本
            ParsedReport fallback = new ParsedReport();
            fallback.setSummary("AI 返回格式异常，原始内容：" + llmResponse.substring(0, Math.min(200, llmResponse.length())));
            fallback.setRawJson(llmResponse);
            fallback.setAgentScore(BigDecimal.ZERO);
            return fallback;
        }
    }

    /** 尝试清理 LLM 可能返回的 markdown 代码块标记 */
    private String extractJson(String raw) {
        if (raw == null) return "{}";
        raw = raw.trim();
        if (raw.startsWith("```")) {
            int firstNewline = raw.indexOf('\n');
            int lastBacktick = raw.lastIndexOf("```");
            if (firstNewline > 0 && lastBacktick > firstNewline) {
                raw = raw.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        // 找第一个 { 和最后一个 } 之间的内容
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private String getText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child != null ? child.asText("") : "";
    }

    private BigDecimal sumDimensions(JsonNode root) {
        JsonNode dims = root.get("dimensions");
        if (dims == null || !dims.isArray()) return BigDecimal.ZERO;
        double total = 0;
        for (JsonNode dim : dims) {
            JsonNode score = dim.get("score");
            if (score != null && score.isNumber()) total += score.doubleValue();
        }
        return BigDecimal.valueOf(Math.min(total, 100));
    }

    @lombok.Data
    public static class ParsedReport {
        private String summary;
        private String advantage;
        private String problem;
        private String suggestion;
        private BigDecimal agentScore;
        /** LLM 返回的完整 JSON 字符串 */
        private String rawJson;
    }
}
