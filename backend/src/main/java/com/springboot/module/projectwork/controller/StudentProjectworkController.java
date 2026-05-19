package com.springboot.module.projectwork.controller;

import com.springboot.common.BaseResponse;
import com.springboot.common.ResultUtils;
import com.springboot.model.entity.system.User;
import com.springboot.model.vo.projectwork.AssignmentVO;
import com.springboot.model.vo.projectwork.EvalReportVO;
import com.springboot.model.vo.projectwork.SubmissionVO;
import com.springboot.module.auth.service.UserService;
import com.springboot.module.projectwork.service.ProjectworkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/student/projectwork")
@RequiredArgsConstructor
public class StudentProjectworkController {

    private final ProjectworkService projectworkService;
    private final UserService userService;

    @GetMapping("/{id}")
    public BaseResponse<AssignmentVO> getHomework(@PathVariable Long id) {
        return ResultUtils.success(projectworkService.getStudentAssignment(id));
    }

    @GetMapping("/{id}/submission")
    public BaseResponse<SubmissionVO> getSubmission(@PathVariable Long id,
                                                    HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        SubmissionVO vo = projectworkService.getStudentSubmission(id, user.getId());
        return ResultUtils.success(vo);
    }

    /**
     * Student uploads ZIP with optional member student IDs.
     * Form fields: file (multipart), memberIds (comma-separated, optional)
     */
    @PostMapping("/{id}/submissions")
    public BaseResponse<Long> uploadSubmission(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "memberIds", required = false) String memberIds,
            HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        List<Long> memberList = Collections.emptyList();
        if (memberIds != null && !memberIds.isBlank()) {
            memberList = Arrays.stream(memberIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        Long submissionId = projectworkService.createSubmission(id, user.getId(), file, memberList);
        return ResultUtils.success(submissionId);
    }

    @GetMapping("/{id}/report")
    public BaseResponse<EvalReportVO> getReport(@PathVariable Long id,
                                                HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(projectworkService.getStudentReport(id, user.getId()));
    }
}
