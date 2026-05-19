package com.springboot.module.system.controller;

import com.springboot.common.BaseResponse;
import com.springboot.common.ResultUtils;
import com.springboot.model.vo.projectwork.StudentSearchVO;
import com.springboot.module.projectwork.service.ProjectworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final ProjectworkService projectworkService;

    @GetMapping("/search")
    public BaseResponse<List<StudentSearchVO>> searchStudents(
            @RequestParam(defaultValue = "") String q) {
        return ResultUtils.success(projectworkService.searchStudents(q));
    }
}
