package com.springboot.model.vo.projectwork;

import java.io.Serializable;
import lombok.Data;

@Data
public class StudentSearchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String studentNo;
    private String studentName;
    private String gender;
    private String className;
}
