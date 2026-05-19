package com.springboot.model.vo.projectwork;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class EvalStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String taskStatus;
    private String submitStatus;
    private String errorMessage;
    private List<StepVO> steps;
    private List<EvalReportVO.ScreenshotVO> screenshots;

    @Data
    public static class StepVO implements Serializable {
        private String name;
        /** pending / running / done / error */
        private String status;
        private String duration;
        private String log;
    }
}
