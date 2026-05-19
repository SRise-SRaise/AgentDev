package com.springboot.model.dto.projectwork;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class BatchEvalRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> submissionIds;
}
