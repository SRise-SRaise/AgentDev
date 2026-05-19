package com.springboot.model.vo.projectwork;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class EvalReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long agentTaskId;
    private Long projectSubmissionId;
    private String reportTitle;
    private String summary;
    private String advantage;
    private String problem;
    private String suggestion;
    private BigDecimal agentScore;
    private String reportJson;

    /** Parsed from report_json dimensions array */
    private List<DimensionVO> dimensions;

    /** Parsed from report_json pros/cons lists */
    private List<String> pros;
    private List<String> cons;

    /** Screenshot urls from eval task output_json */
    private List<ScreenshotVO> screenshots;

    /** Teacher review */
    private boolean reviewed;
    private BigDecimal teacherScore;
    private String reviewComment;
    private String scoreStatus;

    @Data
    public static class DimensionVO implements Serializable {
        private String name;
        private BigDecimal score;
        private Integer total;
        private String reason;
    }

    @Data
    public static class ScreenshotVO implements Serializable {
        private String url;
        private String label;
    }
}
