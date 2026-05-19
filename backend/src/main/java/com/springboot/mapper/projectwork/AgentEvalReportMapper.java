package com.springboot.mapper.projectwork;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springboot.model.entity.projectwork.AgentEvalReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentEvalReportMapper extends BaseMapper<AgentEvalReport> {

    @Select("SELECT * FROM agent_eval_report WHERE project_submission_id = #{submissionId} ORDER BY created_at DESC LIMIT 1")
    AgentEvalReport selectLatestBySubmission(Long submissionId);
}
