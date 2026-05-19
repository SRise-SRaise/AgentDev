package com.springboot.mapper.projectwork;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springboot.model.entity.projectwork.ProjectScore;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectScoreMapper extends BaseMapper<ProjectScore> {

    @Select("SELECT psc.*, s.student_no, s.student_name, s.gender, " +
            "pg.group_name, pg.id AS group_no_id " +
            "FROM project_score psc " +
            "JOIN student s ON s.id = psc.student_id " +
            "LEFT JOIN project_group pg ON pg.id = psc.group_id " +
            "WHERE psc.assignment_id = #{assignmentId} " +
            "ORDER BY pg.id ASC, s.student_no ASC")
    List<java.util.Map<String, Object>> selectGradesByAssignment(Long assignmentId);
}
