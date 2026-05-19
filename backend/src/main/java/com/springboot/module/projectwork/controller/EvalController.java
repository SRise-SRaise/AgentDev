package com.springboot.module.projectwork.controller;

import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.exception.BusinessException;
import com.springboot.model.entity.projectwork.AgentEvalTask;
import com.springboot.module.projectwork.eval.EvalTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Projectwork Agent evaluation endpoints.
 *
 * POST /api/eval/trigger?submissionId=xxx  - trigger evaluation, returns taskId immediately
 * GET  /api/eval/status/{taskId}           - poll evaluation progress
 */
@Slf4j
@RestController
@RequestMapping("/eval")
public class EvalController {

    @Resource
    private EvalTaskService evalTaskService;

    @PostMapping("/trigger")
    public BaseResponse<Long> triggerEval(@RequestParam Long submissionId) {
        if (submissionId == null || submissionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid submissionId");
        }
        Long taskId = evalTaskService.triggerEval(submissionId);
        return ResultUtils.success(taskId);
    }

    @GetMapping("/status/{taskId}")
    public BaseResponse<AgentEvalTask> getTaskStatus(@PathVariable Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid taskId");
        }
        AgentEvalTask task = evalTaskService.getTaskStatus(taskId);
        return ResultUtils.success(task);
    }
}
