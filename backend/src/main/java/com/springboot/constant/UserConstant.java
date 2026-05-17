package com.springboot.constant;

/**
 * 用户常量
*
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "STUDENT";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "ADMIN";

    /**
     * 教师角色
     */
    String TEACHER_ROLE = "TEACHER";

    /**
     * 学生角色
     */
    String STUDENT_ROLE = "STUDENT";

    // endregion
}
