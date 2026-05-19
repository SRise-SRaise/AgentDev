package com.springboot.model.vo.projectwork;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class SubmissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long assignmentId;
    private Long groupId;
    private Integer groupNo;
    private Long submitStudentId;
    private Long zipFileId;
    private String fileName;
    private String fileSize;
    private String submitStatus;
    private Date submitTime;
    private String runLog;
    private String errorMessage;

    /** Group members */
    private List<MemberVO> members;

    /** Latest eval task status */
    private Long evalTaskId;
    private String evalStatus;
    private String evalOutputJson;

    /** Agent score */
    private BigDecimal agentScore;

    /** Teacher review */
    private BigDecimal teacherScore;
    private String reviewComment;
    private String scoreStatus;

    @Data
    public static class MemberVO implements Serializable {
        private Long studentId;
        private String studentName;
        private String studentNo;
        private boolean isLeader;
        private String roleName;
    }
}
