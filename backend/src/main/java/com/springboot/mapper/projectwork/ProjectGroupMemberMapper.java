package com.springboot.mapper.projectwork;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springboot.model.entity.projectwork.ProjectGroupMember;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectGroupMemberMapper extends BaseMapper<ProjectGroupMember> {

    @Select("SELECT pgm.*, s.student_no, s.student_name " +
            "FROM project_group_member pgm " +
            "JOIN student s ON s.id = pgm.student_id " +
            "WHERE pgm.group_id = #{groupId}")
    List<java.util.Map<String, Object>> selectMembersWithInfo(Long groupId);
}
