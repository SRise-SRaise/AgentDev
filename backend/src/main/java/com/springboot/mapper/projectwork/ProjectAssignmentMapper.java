package com.springboot.mapper.projectwork;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springboot.model.entity.projectwork.ProjectAssignment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectAssignmentMapper extends BaseMapper<ProjectAssignment> {

    @Select("SELECT pa.*, " +
            "(SELECT COUNT(*) FROM project_submission ps WHERE ps.assignment_id = pa.id) AS submission_count, " +
            "(SELECT COUNT(*) FROM project_submission ps " +
            " JOIN agent_eval_task aet ON aet.related_id = ps.id AND aet.task_type = 'PROJECT' AND aet.task_status = 'SUCCESS' " +
            " WHERE ps.assignment_id = pa.id) AS eval_count " +
            "FROM project_assignment pa WHERE pa.course_id = #{courseId} ORDER BY pa.created_at DESC")
    List<java.util.Map<String, Object>> selectWithStatsByCourse(Long courseId);
}
