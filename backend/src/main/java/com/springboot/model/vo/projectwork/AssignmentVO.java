package com.springboot.model.vo.projectwork;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class AssignmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private String requirement;
    private String submitFormat;
    private BigDecimal fullScore;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Parsed from rubric_json */
    private List<ScoreItemVO> scoreItems;

    /** Aggregated */
    private Integer submissionCount;
    private Integer evalCount;

    @Data
    public static class ScoreItemVO implements Serializable {
        private String name;
        private Integer weight;
    }
}
