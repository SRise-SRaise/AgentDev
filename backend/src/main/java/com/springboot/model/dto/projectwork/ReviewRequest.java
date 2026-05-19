package com.springboot.model.dto.projectwork;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal teacherScore;

    private String reviewComment;
}
