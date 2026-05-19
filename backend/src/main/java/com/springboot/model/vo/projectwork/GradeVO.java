package com.springboot.model.vo.projectwork;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class GradeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long groupId;
    private Integer groupNo;
    private Long submissionId;
    private BigDecimal agentScore;
    private BigDecimal teacherScore;
    private BigDecimal finalScore;
    private String scoreStatus;
    private String reviewComment;
    private LocalDateTime confirmedAt;

    private List<SubmissionVO.MemberVO> members;
}
