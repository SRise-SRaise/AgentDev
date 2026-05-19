package com.springboot.mapper.projectwork;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springboot.model.entity.projectwork.ProjectSubmission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectSubmissionMapper extends BaseMapper<ProjectSubmission> {

    @Select("SELECT ps.*, " +
            "fr.original_name AS file_name, fr.file_size, " +
            "aet.id AS eval_task_id, aet.task_status AS eval_status, aet.output_json AS eval_output_json, " +
            "psc.agent_score, psc.teacher_score, psc.review_comment, psc.score_status " +
            "FROM project_submission ps " +
            "LEFT JOIN file_resource fr ON fr.id = ps.zip_file_id " +
            "LEFT JOIN agent_eval_task aet ON aet.related_id = ps.id AND aet.task_type = 'PROJECT' " +
            "  AND aet.id = (SELECT MAX(id) FROM agent_eval_task WHERE related_id = ps.id AND task_type = 'PROJECT') " +
            "LEFT JOIN project_score psc ON psc.submission_id = ps.id AND psc.student_id = ps.submit_student_id " +
            "WHERE ps.assignment_id = #{assignmentId} ORDER BY ps.submit_time DESC")
    List<java.util.Map<String, Object>> selectDetailsByAssignment(Long assignmentId);

    @Select("SELECT ps.*, " +
            "fr.original_name AS file_name, fr.file_size, " +
            "aet.id AS eval_task_id, aet.task_status AS eval_status, aet.output_json AS eval_output_json, " +
            "psc.agent_score, psc.teacher_score, psc.review_comment, psc.score_status " +
            "FROM project_submission ps " +
            "LEFT JOIN file_resource fr ON fr.id = ps.zip_file_id " +
            "LEFT JOIN agent_eval_task aet ON aet.related_id = ps.id AND aet.task_type = 'PROJECT' " +
            "  AND aet.id = (SELECT MAX(id) FROM agent_eval_task WHERE related_id = ps.id AND task_type = 'PROJECT') " +
            "LEFT JOIN project_score psc ON psc.submission_id = ps.id AND psc.student_id = ps.submit_student_id " +
            "WHERE ps.id = #{submissionId}")
    java.util.Map<String, Object> selectDetailById(Long submissionId);

    @Select("SELECT ps.id FROM project_submission ps " +
            "WHERE ps.assignment_id = #{assignmentId} AND ps.submit_student_id = " +
            "(SELECT id FROM student WHERE user_id = #{userId} LIMIT 1) " +
            "ORDER BY ps.submit_time DESC LIMIT 1")
    Long findLatestIdByAssignmentAndUser(Long assignmentId, Long userId);
}
