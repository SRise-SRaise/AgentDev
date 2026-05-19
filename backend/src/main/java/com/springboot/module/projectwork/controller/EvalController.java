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
 * 大作业 Agent 评测接口
 *
 * POST /api/eval/trigger?submissionId=xxx   触发评测，立即返回 taskId
 * GET  /api/eval/status/{taskId}            查询评测进度（前端轮询）
 */
@Slf4j
@RestController
@RequestMapping("/eval")
public class EvalController {

    @Resource
    private EvalTaskService evalTaskService;

    /**
     * 触发 Agent 评测
     * @param submissionId 大作业提交 ID
     * @return agent_eval_task.id（任务 ID，用于轮询状态）
     */
    @PostMapping("/trigger")
    public BaseResponse<Long> triggerEval(@RequestParam Long submissionId) {
        if (submissionId == null || submissionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "submissionId 不合法");
        }
        Long taskId = evalTaskService.triggerEval(submissionId);
        return ResultUtils.success(taskId);
    }

    /**
     * 查询评测任务状态（前端轮询接口）
     * task_status: PENDING / RUNNING / SUCCESS / FAILED
     * output_json.steps: 步骤进度列表
     */
    @GetMapping("/status/{taskId}")
    public BaseResponse<AgentEvalTask> getTaskStatus(@PathVariable Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "taskId 不合法");
        }
        AgentEvalTask task = evalTaskService.getTaskStatus(taskId);
        return ResultUtils.success(task);
    }
}
