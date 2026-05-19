package com.springboot.mapper.projectwork;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springboot.model.entity.projectwork.ProjectGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectGroupMapper extends BaseMapper<ProjectGroup> {

    @Select("SELECT MAX(id) FROM project_group WHERE assignment_id = #{assignmentId}")
    Long selectMaxIdByAssignment(Long assignmentId);
}
