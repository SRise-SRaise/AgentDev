package com.springboot.model.dto.projectwork;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class AssignmentCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long courseId;

    private String title;

    private String description;

    private String requirement;

    private String submitFormat;

    private String startTime;

    private String deadline;

    /** List of {name, weight} objects */
    private List<ScoreItemDTO> scoreItems;

    @Data
    public static class ScoreItemDTO implements Serializable {
        private String name;
        private Integer weight;
    }
}
