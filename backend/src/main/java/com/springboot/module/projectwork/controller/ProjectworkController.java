package com.springboot.module.projectwork.controller;

import com.springboot.common.BaseResponse;
import com.springboot.common.ResultUtils;
import com.springboot.model.dto.projectwork.AssignmentCreateRequest;
import com.springboot.model.dto.projectwork.BatchEvalRequest;
import com.springboot.model.dto.projectwork.ReviewRequest;
import com.springboot.model.entity.system.User;
import com.springboot.model.vo.projectwork.*;
import com.springboot.module.auth.service.UserService;
import com.springboot.module.projectwork.eval.EvalTaskService;
import com.springboot.module.projectwork.service.ProjectworkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/projectwork")
@RequiredArgsConstructor
public class ProjectworkController {

    private final ProjectworkService projectworkService;
    private final EvalTaskService evalTaskService;
    private final UserService userService;

    // ---- Assignment CRUD ----

    @GetMapping
    public BaseResponse<List<AssignmentVO>> listProjectworks(
            @RequestParam(defaultValue = "1") Long courseId) {
        return ResultUtils.success(projectworkService.listAssignments(courseId));
    }

    @GetMapping("/{id}")
    public BaseResponse<AssignmentVO> getProjectwork(@PathVariable Long id) {
        return ResultUtils.success(projectworkService.getAssignment(id));
    }

    @PostMapping
    public BaseResponse<Long> createProjectwork(@RequestBody AssignmentCreateRequest req,
                                                HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        Long id = projectworkService.createAssignment(req, user.getId());
        return ResultUtils.success(id);
    }

    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateProjectwork(@PathVariable Long id,
                                                   @RequestBody AssignmentCreateRequest req) {
        projectworkService.updateAssignment(id, req);
        return ResultUtils.success(true);
    }

    @PostMapping("/{id}/publish")
    public BaseResponse<Boolean> publishProjectwork(@PathVariable Long id) {
        projectworkService.publishAssignment(id);
        return ResultUtils.success(true);
    }

    // ---- Submission management ----

    @GetMapping("/{id}/submissions")
    public BaseResponse<List<SubmissionVO>> listSubmissions(@PathVariable Long id) {
        return ResultUtils.success(projectworkService.listSubmissions(id));
    }

    @GetMapping("/{id}/submissions/{sid}")
    public BaseResponse<SubmissionVO> getSubmission(@PathVariable Long id,
                                                    @PathVariable Long sid) {
        return ResultUtils.success(projectworkService.getSubmission(id, sid));
    }

    // ---- Eval trigger ----

    @PostMapping("/{id}/submissions/{sid}/evaluate")
    public BaseResponse<Long> triggerEval(@PathVariable Long id, @PathVariable Long sid) {
        Long taskId = evalTaskService.triggerEval(sid);
        return ResultUtils.success(taskId);
    }

    @PostMapping("/{id}/evaluate/batch")
    public BaseResponse<Boolean> batchTriggerEval(@PathVariable Long id,
                                                  @RequestBody BatchEvalRequest req) {
        if (req.getSubmissionIds() != null) {
            for (Long sid : req.getSubmissionIds()) {
                try {
                    evalTaskService.triggerEval(sid);
                } catch (Exception e) {
                    log.warn("[ProjectworkController] Batch eval skip sid={}: {}", sid, e.getMessage());
                }
            }
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/{id}/submissions/{sid}/eval-status")
    public BaseResponse<EvalStatusVO> getEvalStatus(@PathVariable Long id,
                                                    @PathVariable Long sid) {
        return ResultUtils.success(projectworkService.getEvalStatus(id, sid));
    }

    // ---- Report ----

    @GetMapping("/{id}/submissions/{sid}/report")
    public BaseResponse<EvalReportVO> getReport(@PathVariable Long id,
                                                @PathVariable Long sid) {
        return ResultUtils.success(projectworkService.getEvalReport(id, sid));
    }

    // ---- Teacher review ----

    @PostMapping("/{id}/submissions/{sid}/review")
    public BaseResponse<Boolean> saveReview(@PathVariable Long id, @PathVariable Long sid,
                                            @RequestBody ReviewRequest req) {
        projectworkService.saveReview(id, sid, req);
        return ResultUtils.success(true);
    }

    // ---- Grades ----

    @GetMapping("/{id}/grades")
    public BaseResponse<List<GradeVO>> getGrades(@PathVariable Long id) {
        return ResultUtils.success(projectworkService.listGrades(id));
    }
}
