-- 基于 Agent 的 Web 高级编程管理平台数据库初始化脚本
-- 说明：
-- 1. 本文件整合了全部建表和推荐初始化数据
-- 2. 按模块使用大段注释划分，便于团队和 AI 快速定位
-- 3. 推荐在 MySQL 8.x 环境执行

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS web_advanced_platform
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE web_advanced_platform;

/* ============================================================================
   一、初始化清理
   说明：清理当前平台表和旧模板表，保证脚本可重复执行
   ============================================================================ */

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS export_task;
DROP TABLE IF EXISTS file_resource;
DROP TABLE IF EXISTS project_score;
DROP TABLE IF EXISTS agent_eval_report;
DROP TABLE IF EXISTS agent_eval_task;
DROP TABLE IF EXISTS project_submission;
DROP TABLE IF EXISTS project_group_member;
DROP TABLE IF EXISTS project_group;
DROP TABLE IF EXISTS project_assignment;
DROP TABLE IF EXISTS exp_vue_screenshot;
DROP TABLE IF EXISTS exp_vue_run_config;
DROP TABLE IF EXISTS exp_python_case_result;
DROP TABLE IF EXISTS exp_python_test_case;
DROP TABLE IF EXISTS experiment_score;
DROP TABLE IF EXISTS experiment_submission;
DROP TABLE IF EXISTS experiment_task;
DROP TABLE IF EXISTS course_quality_report;
DROP TABLE IF EXISTS course_attainment_result;
DROP TABLE IF EXISTS course_student_score;
DROP TABLE IF EXISTS homework_score_detail;
DROP TABLE IF EXISTS homework_import_batch;
DROP TABLE IF EXISTS objective_assessment_map;
DROP TABLE IF EXISTS assessment_item;
DROP TABLE IF EXISTS course_objective;
DROP TABLE IF EXISTS course_syllabus_import;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS class_info;
DROP TABLE IF EXISTS sys_user;

DROP TABLE IF EXISTS post_favour;
DROP TABLE IF EXISTS post_thumb;
DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS user;

SET FOREIGN_KEY_CHECKS = 1;

/* ============================================================================
   二、基础用户模块
   包含：sys_user、class_info、student、teacher
   ============================================================================ */

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '登录账号',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    role VARCHAR(32) NOT NULL COMMENT '角色：TEACHER/STUDENT/ADMIN',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='系统用户表';

CREATE TABLE class_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
    class_name VARCHAR(128) NOT NULL COMMENT '班级名称',
    major VARCHAR(128) DEFAULT '软件工程' COMMENT '专业',
    grade_year VARCHAR(16) DEFAULT NULL COMMENT '年级',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='班级信息表';

CREATE TABLE student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID',
    user_id BIGINT DEFAULT NULL COMMENT '关联用户ID',
    student_no VARCHAR(64) NOT NULL UNIQUE COMMENT '学号',
    student_name VARCHAR(64) NOT NULL COMMENT '姓名',
    gender VARCHAR(16) DEFAULT NULL COMMENT '性别',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_class (class_id),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES class_info(id)
) COMMENT='学生信息表';

CREATE TABLE teacher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '教师ID',
    user_id BIGINT NOT NULL COMMENT '关联用户ID',
    teacher_no VARCHAR(64) DEFAULT NULL COMMENT '教师工号',
    teacher_name VARCHAR(64) NOT NULL COMMENT '教师姓名',
    department VARCHAR(128) DEFAULT NULL COMMENT '所属院系/教研室',
    title VARCHAR(64) DEFAULT NULL COMMENT '职称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_teacher_user (user_id),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT='教师信息表';

/* ============================================================================
   三、课程闭环与成绩管理模块
   包含：course、course_syllabus_import、course_objective、assessment_item、
        objective_assessment_map、homework_import_batch、homework_score_detail、
        course_student_score、course_attainment_result、course_quality_report
   ============================================================================ */

CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_name VARCHAR(128) NOT NULL DEFAULT 'Web 高级编程' COMMENT '课程名称',
    course_code VARCHAR(64) DEFAULT NULL COMMENT '课程编号',
    semester VARCHAR(64) DEFAULT NULL COMMENT '学期',
    credit DECIMAL(4,1) DEFAULT NULL COMMENT '学分',
    total_hours INT DEFAULT NULL COMMENT '总学时',
    theory_hours INT DEFAULT NULL COMMENT '理论学时',
    experiment_hours INT DEFAULT NULL COMMENT '实验学时',
    teacher_id BIGINT DEFAULT NULL COMMENT '任课教师ID',
    class_id BIGINT DEFAULT NULL COMMENT '授课班级ID',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_semester (semester),
    INDEX idx_course_teacher (teacher_id),
    INDEX idx_course_class (class_id),
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT fk_course_class FOREIGN KEY (class_id) REFERENCES class_info(id)
) COMMENT='课程表';

CREATE TABLE course_syllabus_import (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '导入记录ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    file_id BIGINT NOT NULL COMMENT '课程大纲文件ID',
    parse_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/CONFIRMED',
    raw_text LONGTEXT DEFAULT NULL COMMENT '解析出的原始文本',
    parsed_json JSON DEFAULT NULL COMMENT '结构化解析结果',
    error_message TEXT DEFAULT NULL COMMENT '解析失败原因',
    confirmed_by BIGINT DEFAULT NULL COMMENT '确认教师用户ID',
    confirmed_at DATETIME DEFAULT NULL COMMENT '确认时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_syllabus_course (course_id),
    INDEX idx_syllabus_file (file_id),
    CONSTRAINT fk_syllabus_course FOREIGN KEY (course_id) REFERENCES course(id)
) COMMENT='课程大纲导入表';

CREATE TABLE course_objective (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程目标ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    objective_code VARCHAR(32) NOT NULL COMMENT '课程目标编号，如 课程目标1',
    objective_type VARCHAR(32) DEFAULT NULL COMMENT '知识目标/能力目标/素质目标',
    description TEXT NOT NULL COMMENT '课程目标描述',
    graduation_requirement VARCHAR(255) DEFAULT NULL COMMENT '支撑毕业要求',
    expected_rate DECIMAL(5,2) NOT NULL DEFAULT 0.70 COMMENT '达成阈值，例如0.70',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_objective_course (course_id),
    UNIQUE KEY uk_course_objective_code (course_id, objective_code),
    CONSTRAINT fk_objective_course FOREIGN KEY (course_id) REFERENCES course(id)
) COMMENT='课程目标表';

CREATE TABLE assessment_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评分项ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    item_name VARCHAR(64) NOT NULL COMMENT '评分项名称：作业/实验/大作业',
    item_type VARCHAR(32) NOT NULL COMMENT 'HOMEWORK/EXPERIMENT/PROJECT',
    full_score DECIMAL(6,2) NOT NULL COMMENT '该项满分，如30、20、50',
    weight DECIMAL(5,2) NOT NULL COMMENT '权重，如0.30、0.20、0.50',
    source_type VARCHAR(32) NOT NULL COMMENT 'MANUAL/EXPERIMENT_MODULE/PROJECT_MODULE',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    description TEXT DEFAULT NULL COMMENT '说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_assessment_course (course_id),
    UNIQUE KEY uk_course_item_type (course_id, item_type),
    CONSTRAINT fk_assessment_course FOREIGN KEY (course_id) REFERENCES course(id)
) COMMENT='课程评分项表';

CREATE TABLE objective_assessment_map (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '映射ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    objective_id BIGINT NOT NULL COMMENT '课程目标ID',
    assessment_item_id BIGINT NOT NULL COMMENT '评分项ID',
    objective_score DECIMAL(6,2) NOT NULL COMMENT '该评分项分配给该课程目标的满分',
    description TEXT DEFAULT NULL COMMENT '说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_map_course (course_id),
    INDEX idx_map_objective (objective_id),
    INDEX idx_map_item (assessment_item_id),
    UNIQUE KEY uk_objective_item (objective_id, assessment_item_id),
    CONSTRAINT fk_map_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_map_objective FOREIGN KEY (objective_id) REFERENCES course_objective(id),
    CONSTRAINT fk_map_item FOREIGN KEY (assessment_item_id) REFERENCES assessment_item(id)
) COMMENT='课程目标与评分项映射表';

CREATE TABLE homework_import_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '导入批次ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    file_id BIGINT DEFAULT NULL COMMENT '导入Excel文件ID',
    import_user_id BIGINT NOT NULL COMMENT '导入教师用户ID',
    total_count INT NOT NULL DEFAULT 0 COMMENT '总记录数',
    success_count INT NOT NULL DEFAULT 0 COMMENT '成功数',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败数',
    import_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/PARTIAL/FAILED',
    error_message TEXT DEFAULT NULL COMMENT '导入异常说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_homework_batch_course (course_id),
    CONSTRAINT fk_homework_batch_course FOREIGN KEY (course_id) REFERENCES course(id)
) COMMENT='作业成绩导入批次表';

CREATE TABLE homework_score_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作业成绩明细ID',
    batch_id BIGINT NOT NULL COMMENT '导入批次ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    homework_score DECIMAL(6,2) NOT NULL COMMENT '作业成绩，满分30',
    raw_student_no VARCHAR(64) DEFAULT NULL COMMENT 'Excel中的学号',
    raw_student_name VARCHAR(64) DEFAULT NULL COMMENT 'Excel中的姓名',
    import_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
    error_message TEXT DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_homework_course_student (course_id, student_id),
    CONSTRAINT fk_homework_detail_batch FOREIGN KEY (batch_id) REFERENCES homework_import_batch(id),
    CONSTRAINT fk_homework_detail_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_homework_detail_student FOREIGN KEY (student_id) REFERENCES student(id)
) COMMENT='作业成绩导入明细表';

CREATE TABLE course_student_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成绩ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    homework_score DECIMAL(6,2) DEFAULT NULL COMMENT '作业成绩，满分30，教师导入',
    experiment_raw_avg DECIMAL(6,2) DEFAULT NULL COMMENT '实验原始平均分，满分100',
    experiment_score DECIMAL(6,2) DEFAULT NULL COMMENT '实验折算分，满分20',
    project_raw_score DECIMAL(6,2) DEFAULT NULL COMMENT '大作业原始分，满分100',
    project_score DECIMAL(6,2) DEFAULT NULL COMMENT '大作业折算分，满分50',
    total_score DECIMAL(6,2) DEFAULT NULL COMMENT '总成绩，满分100',
    objective1_score DECIMAL(6,2) DEFAULT NULL COMMENT '课程目标1得分',
    objective2_score DECIMAL(6,2) DEFAULT NULL COMMENT '课程目标2得分',
    objective1_achieved TINYINT DEFAULT NULL COMMENT '课程目标1是否达成',
    objective2_achieved TINYINT DEFAULT NULL COMMENT '课程目标2是否达成',
    score_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CALCULATED/LOCKED',
    last_calculated_at DATETIME DEFAULT NULL COMMENT '最近计算时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_student (course_id, student_id),
    INDEX idx_score_course (course_id),
    INDEX idx_score_student (student_id),
    CONSTRAINT fk_score_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES student(id)
) COMMENT='学生课程成绩汇总表';

CREATE TABLE course_attainment_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '达成度结果ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    objective_id BIGINT DEFAULT NULL COMMENT '课程目标ID，NULL表示总体',
    target_full_score DECIMAL(6,2) NOT NULL COMMENT '该课程目标总分',
    avg_score DECIMAL(6,2) DEFAULT NULL COMMENT '学生平均得分',
    attainment_rate DECIMAL(6,4) DEFAULT NULL COMMENT '达成度比例',
    expected_rate DECIMAL(6,4) DEFAULT NULL COMMENT '目标阈值',
    achieved TINYINT DEFAULT NULL COMMENT '是否达成',
    calculate_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_attainment_course (course_id),
    INDEX idx_attainment_objective (objective_id),
    CONSTRAINT fk_attainment_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_attainment_objective FOREIGN KEY (objective_id) REFERENCES course_objective(id)
) COMMENT='课程目标达成度结果表';

CREATE TABLE course_quality_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资料汇编ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    report_title VARCHAR(255) NOT NULL COMMENT '报告标题',
    report_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/GENERATED/EXPORTED',
    summary TEXT DEFAULT NULL COMMENT '课程质量评价摘要',
    problem_analysis TEXT DEFAULT NULL COMMENT '问题分析',
    improvement_measures TEXT DEFAULT NULL COMMENT '改进措施',
    report_json JSON DEFAULT NULL COMMENT '结构化报告内容',
    file_id BIGINT DEFAULT NULL COMMENT '导出文件ID',
    generated_by BIGINT DEFAULT NULL COMMENT '生成教师用户ID',
    generated_at DATETIME DEFAULT NULL COMMENT '生成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_quality_course (course_id),
    CONSTRAINT fk_quality_course FOREIGN KEY (course_id) REFERENCES course(id)
) COMMENT='课程质量评价资料汇编表';

/* ============================================================================
   四、实验公共模块
   包含：experiment_task、experiment_submission、experiment_score
   ============================================================================ */

CREATE TABLE experiment_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    experiment_type VARCHAR(32) NOT NULL COMMENT 'PYTHON/VUE',
    title VARCHAR(255) NOT NULL COMMENT '实验标题',
    description TEXT DEFAULT NULL COMMENT '实验说明',
    requirement TEXT DEFAULT NULL COMMENT '实验要求',
    full_score DECIMAL(6,2) NOT NULL DEFAULT 100.00 COMMENT '实验满分，默认100',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    required_flag TINYINT NOT NULL DEFAULT 1 COMMENT '是否计入实验平均分',
    submit_type VARCHAR(32) NOT NULL COMMENT 'CODE/ZIP',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    deadline DATETIME DEFAULT NULL COMMENT '截止时间',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CLOSED',
    created_by BIGINT DEFAULT NULL COMMENT '创建教师用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_exp_task_course (course_id),
    INDEX idx_exp_task_type (experiment_type),
    INDEX idx_exp_task_status (status),
    CONSTRAINT fk_exp_task_course FOREIGN KEY (course_id) REFERENCES course(id)
) COMMENT='实验任务主表';

CREATE TABLE experiment_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验提交ID',
    experiment_id BIGINT NOT NULL COMMENT '实验任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    submit_type VARCHAR(32) NOT NULL COMMENT 'CODE/ZIP',
    code_content LONGTEXT DEFAULT NULL COMMENT '代码内容，主要用于Python',
    file_id BIGINT DEFAULT NULL COMMENT '上传文件ID，主要用于Vue ZIP',
    submit_status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/RUNNING/SUCCESS/FAILED/REVIEWED',
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    run_log LONGTEXT DEFAULT NULL COMMENT '整体运行日志',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_submission_exp (experiment_id),
    INDEX idx_submission_student (student_id),
    INDEX idx_submission_course_student (course_id, student_id),
    CONSTRAINT fk_submission_exp FOREIGN KEY (experiment_id) REFERENCES experiment_task(id),
    CONSTRAINT fk_submission_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES student(id)
) COMMENT='实验提交主表';

CREATE TABLE experiment_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实验成绩ID',
    experiment_id BIGINT NOT NULL COMMENT '实验任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    submission_id BIGINT DEFAULT NULL COMMENT '采用的提交ID',
    auto_score DECIMAL(6,2) DEFAULT NULL COMMENT '系统自动评分',
    teacher_score DECIMAL(6,2) DEFAULT NULL COMMENT '教师复核分',
    final_score DECIMAL(6,2) DEFAULT NULL COMMENT '最终分，满分100',
    score_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/AUTO_SCORED/REVIEWED/CONFIRMED',
    score_source VARCHAR(32) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/TEACHER/MIXED',
    review_comment TEXT DEFAULT NULL COMMENT '教师反馈',
    confirmed_at DATETIME DEFAULT NULL COMMENT '确认时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exp_student (experiment_id, student_id),
    INDEX idx_exp_score_course_student (course_id, student_id),
    INDEX idx_exp_score_status (score_status),
    CONSTRAINT fk_exp_score_exp FOREIGN KEY (experiment_id) REFERENCES experiment_task(id),
    CONSTRAINT fk_exp_score_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_exp_score_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_exp_score_submission FOREIGN KEY (submission_id) REFERENCES experiment_submission(id)
) COMMENT='实验最终成绩表';

/* ============================================================================
   五、Python 实验模块
   包含：exp_python_test_case、exp_python_case_result
   ============================================================================ */

CREATE TABLE exp_python_test_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '测试用例ID',
    experiment_id BIGINT NOT NULL COMMENT '实验任务ID',
    case_name VARCHAR(128) NOT NULL COMMENT '用例名称',
    input_data TEXT DEFAULT NULL COMMENT '输入数据',
    expected_output TEXT DEFAULT NULL COMMENT '期望输出',
    score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '该用例分值',
    is_hidden TINYINT NOT NULL DEFAULT 0 COMMENT '是否隐藏用例',
    time_limit_ms INT NOT NULL DEFAULT 3000 COMMENT '时间限制毫秒',
    memory_limit_mb INT NOT NULL DEFAULT 128 COMMENT '内存限制MB',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_py_case_exp (experiment_id),
    CONSTRAINT fk_py_case_exp FOREIGN KEY (experiment_id) REFERENCES experiment_task(id)
) COMMENT='Python 实验测试用例表';

CREATE TABLE exp_python_case_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Python用例结果ID',
    submission_id BIGINT NOT NULL COMMENT '实验提交ID',
    test_case_id BIGINT NOT NULL COMMENT '测试用例ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    actual_output TEXT DEFAULT NULL COMMENT '实际输出',
    expected_output TEXT DEFAULT NULL COMMENT '期望输出快照',
    passed TINYINT NOT NULL DEFAULT 0 COMMENT '是否通过',
    score DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '本用例得分',
    time_used_ms INT DEFAULT NULL COMMENT '运行耗时',
    memory_used_mb INT DEFAULT NULL COMMENT '内存占用',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_py_result_submission (submission_id),
    INDEX idx_py_result_case (test_case_id),
    INDEX idx_py_result_student (student_id),
    CONSTRAINT fk_py_result_submission FOREIGN KEY (submission_id) REFERENCES experiment_submission(id),
    CONSTRAINT fk_py_result_case FOREIGN KEY (test_case_id) REFERENCES exp_python_test_case(id),
    CONSTRAINT fk_py_result_student FOREIGN KEY (student_id) REFERENCES student(id)
) COMMENT='Python 单测试用例运行结果表';

/* ============================================================================
   六、Vue 实验模块
   包含：exp_vue_run_config、exp_vue_screenshot
   ============================================================================ */

CREATE TABLE exp_vue_run_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Vue运行配置ID',
    experiment_id BIGINT NOT NULL COMMENT '实验任务ID',
    package_manager VARCHAR(32) NOT NULL DEFAULT 'npm' COMMENT 'npm/pnpm/yarn',
    install_command VARCHAR(255) NOT NULL DEFAULT 'npm install' COMMENT '依赖安装命令',
    start_command VARCHAR(255) NOT NULL DEFAULT 'npm run dev -- --host 0.0.0.0' COMMENT '启动命令',
    dev_port INT NOT NULL DEFAULT 5173 COMMENT '项目端口',
    health_check_path VARCHAR(255) NOT NULL DEFAULT '/' COMMENT '健康检查路径',
    screenshot_paths JSON DEFAULT NULL COMMENT '需要截图的路由数组，如["/","/login"]',
    timeout_seconds INT NOT NULL DEFAULT 120 COMMENT '运行超时时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vue_config_exp (experiment_id),
    CONSTRAINT fk_vue_config_exp FOREIGN KEY (experiment_id) REFERENCES experiment_task(id)
) COMMENT='Vue实验运行配置表';

CREATE TABLE exp_vue_screenshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Vue截图ID',
    submission_id BIGINT NOT NULL COMMENT '实验提交ID',
    experiment_id BIGINT NOT NULL COMMENT '实验任务ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    route_path VARCHAR(255) NOT NULL COMMENT '截图路由',
    screenshot_file_id BIGINT NOT NULL COMMENT '截图文件ID',
    screenshot_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
    page_title VARCHAR(255) DEFAULT NULL COMMENT '页面标题',
    error_message TEXT DEFAULT NULL COMMENT '截图失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vue_shot_submission (submission_id),
    INDEX idx_vue_shot_exp (experiment_id),
    INDEX idx_vue_shot_student (student_id),
    CONSTRAINT fk_vue_shot_submission FOREIGN KEY (submission_id) REFERENCES experiment_submission(id),
    CONSTRAINT fk_vue_shot_exp FOREIGN KEY (experiment_id) REFERENCES experiment_task(id),
    CONSTRAINT fk_vue_shot_student FOREIGN KEY (student_id) REFERENCES student(id)
) COMMENT='Vue 实验截图结果表';

/* ============================================================================
   七、大作业与 Agent 评测模块
   包含：project_assignment、project_group、project_group_member、project_submission、
        agent_eval_task、agent_eval_report、project_score
   ============================================================================ */

CREATE TABLE project_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '大作业ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    title VARCHAR(255) NOT NULL COMMENT '大作业标题',
    description TEXT DEFAULT NULL COMMENT '大作业说明',
    requirement TEXT DEFAULT NULL COMMENT '项目要求',
    rubric_json JSON DEFAULT NULL COMMENT '评分标准JSON',
    full_score DECIMAL(6,2) NOT NULL DEFAULT 100 COMMENT '原始满分100',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    deadline DATETIME DEFAULT NULL COMMENT '截止时间',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CLOSED',
    created_by BIGINT DEFAULT NULL COMMENT '创建教师用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_course (course_id),
    CONSTRAINT fk_project_course FOREIGN KEY (course_id) REFERENCES course(id)
) COMMENT='大作业任务表';

CREATE TABLE project_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '小组ID',
    assignment_id BIGINT NOT NULL COMMENT '大作业ID',
    group_name VARCHAR(128) NOT NULL COMMENT '小组名称',
    leader_student_id BIGINT DEFAULT NULL COMMENT '组长学生ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_group_assignment (assignment_id),
    CONSTRAINT fk_project_group_assignment FOREIGN KEY (assignment_id) REFERENCES project_assignment(id),
    CONSTRAINT fk_project_group_leader FOREIGN KEY (leader_student_id) REFERENCES student(id)
) COMMENT='大作业小组表';

CREATE TABLE project_group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员ID',
    group_id BIGINT NOT NULL COMMENT '小组ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    role_name VARCHAR(64) DEFAULT NULL COMMENT '组内角色',
    contribution_ratio DECIMAL(5,2) DEFAULT 1.00 COMMENT '贡献系数，默认1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_student (group_id, student_id),
    INDEX idx_group_member_student (student_id),
    CONSTRAINT fk_group_member_group FOREIGN KEY (group_id) REFERENCES project_group(id),
    CONSTRAINT fk_group_member_student FOREIGN KEY (student_id) REFERENCES student(id)
) COMMENT='大作业小组成员表';

CREATE TABLE project_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '大作业提交ID',
    assignment_id BIGINT NOT NULL COMMENT '大作业ID',
    group_id BIGINT DEFAULT NULL COMMENT '小组ID',
    submit_student_id BIGINT NOT NULL COMMENT '提交人学生ID',
    zip_file_id BIGINT NOT NULL COMMENT '项目ZIP文件ID',
    submit_status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/RUNNING/EVALUATED/FAILED/REVIEWED',
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    run_log LONGTEXT DEFAULT NULL COMMENT '运行日志',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_submission_assignment (assignment_id),
    INDEX idx_project_submission_group (group_id),
    CONSTRAINT fk_project_submission_assignment FOREIGN KEY (assignment_id) REFERENCES project_assignment(id),
    CONSTRAINT fk_project_submission_group FOREIGN KEY (group_id) REFERENCES project_group(id),
    CONSTRAINT fk_project_submission_student FOREIGN KEY (submit_student_id) REFERENCES student(id)
) COMMENT='大作业提交表';

CREATE TABLE agent_eval_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Agent任务ID',
    task_type VARCHAR(32) NOT NULL COMMENT 'PROJECT/VUE/PYTHON',
    related_id BIGINT NOT NULL COMMENT '关联业务ID，例如project_submission.id',
    agent_name VARCHAR(128) NOT NULL DEFAULT '基于Agent的多文件实验项目评测' COMMENT 'Agent名称',
    task_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    prompt TEXT DEFAULT NULL COMMENT 'Agent评测提示词',
    input_json JSON DEFAULT NULL COMMENT '输入参数',
    output_json JSON DEFAULT NULL COMMENT '输出结果',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_task_type_related (task_type, related_id),
    INDEX idx_agent_task_status (task_status)
) COMMENT='Agent评测任务表';

CREATE TABLE agent_eval_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Agent评测报告ID',
    agent_task_id BIGINT NOT NULL COMMENT 'Agent任务ID',
    project_submission_id BIGINT DEFAULT NULL COMMENT '大作业提交ID',
    report_title VARCHAR(255) NOT NULL COMMENT '报告标题',
    summary TEXT DEFAULT NULL COMMENT '总体评价',
    advantage TEXT DEFAULT NULL COMMENT '优点',
    problem TEXT DEFAULT NULL COMMENT '问题',
    suggestion TEXT DEFAULT NULL COMMENT '改进建议',
    agent_score DECIMAL(6,2) DEFAULT NULL COMMENT 'Agent建议分，满分100',
    report_json JSON DEFAULT NULL COMMENT '结构化报告JSON',
    report_file_id BIGINT DEFAULT NULL COMMENT '报告文件ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_report_task (agent_task_id),
    INDEX idx_agent_report_submission (project_submission_id),
    CONSTRAINT fk_agent_report_task FOREIGN KEY (agent_task_id) REFERENCES agent_eval_task(id),
    CONSTRAINT fk_agent_report_submission FOREIGN KEY (project_submission_id) REFERENCES project_submission(id)
) COMMENT='Agent评测报告表';

CREATE TABLE project_score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '大作业成绩ID',
    assignment_id BIGINT NOT NULL COMMENT '大作业ID',
    group_id BIGINT DEFAULT NULL COMMENT '小组ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    submission_id BIGINT DEFAULT NULL COMMENT '采用的提交ID',
    agent_score DECIMAL(6,2) DEFAULT NULL COMMENT 'Agent建议分，满分100',
    teacher_score DECIMAL(6,2) DEFAULT NULL COMMENT '教师复核分，满分100',
    final_score DECIMAL(6,2) DEFAULT NULL COMMENT '最终分，满分100',
    score_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/AGENT_SCORED/REVIEWED/CONFIRMED',
    review_comment TEXT DEFAULT NULL COMMENT '教师反馈',
    confirmed_at DATETIME DEFAULT NULL COMMENT '确认时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_student (assignment_id, student_id),
    INDEX idx_project_score_assignment (assignment_id),
    INDEX idx_project_score_student (student_id),
    CONSTRAINT fk_project_score_assignment FOREIGN KEY (assignment_id) REFERENCES project_assignment(id),
    CONSTRAINT fk_project_score_group FOREIGN KEY (group_id) REFERENCES project_group(id),
    CONSTRAINT fk_project_score_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_project_score_submission FOREIGN KEY (submission_id) REFERENCES project_submission(id)
) COMMENT='大作业最终成绩表';

/* ============================================================================
   八、文件、导出与日志模块
   包含：file_resource、export_task、operation_log
   ============================================================================ */

CREATE TABLE file_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型：SYLLABUS/HOMEWORK_EXCEL/VUE_ZIP/PROJECT_ZIP/SCREENSHOT/REPORT',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_ext VARCHAR(32) DEFAULT NULL COMMENT '文件扩展名',
    mime_type VARCHAR(128) DEFAULT NULL COMMENT 'MIME类型',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小',
    file_hash VARCHAR(128) DEFAULT NULL COMMENT '文件哈希',
    storage_path VARCHAR(512) NOT NULL COMMENT '存储路径',
    upload_user_id BIGINT DEFAULT NULL COMMENT '上传用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_file_biz_type (biz_type),
    INDEX idx_file_hash (file_hash)
) COMMENT='文件资源表';

CREATE TABLE export_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '导出任务ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    export_type VARCHAR(64) NOT NULL COMMENT 'SCORE_EXCEL/QUALITY_REPORT',
    export_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    file_id BIGINT DEFAULT NULL COMMENT '导出文件ID',
    error_message TEXT DEFAULT NULL COMMENT '失败原因',
    created_by BIGINT DEFAULT NULL COMMENT '创建用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME DEFAULT NULL COMMENT '完成时间',
    INDEX idx_export_course (course_id),
    INDEX idx_export_status (export_status),
    CONSTRAINT fk_export_course FOREIGN KEY (course_id) REFERENCES course(id)
) COMMENT='导出任务表';

CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    module_name VARCHAR(64) NOT NULL COMMENT '模块名称',
    operation_name VARCHAR(128) NOT NULL COMMENT '操作名称',
    biz_id BIGINT DEFAULT NULL COMMENT '业务ID',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
    request_params JSON DEFAULT NULL COMMENT '请求参数',
    result_status VARCHAR(32) DEFAULT NULL COMMENT 'SUCCESS/FAILED',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_user (user_id),
    INDEX idx_log_module (module_name),
    INDEX idx_log_created (created_at)
) COMMENT='操作日志表';

/* ============================================================================
   九、推荐初始化数据
   说明：对应数据库设计文档中的推荐初始化数据
   ============================================================================ */

-- 9.1 基础账号数据
-- 说明：
-- 1. 当前密码按现有后端的盐值规则生成：MD5('springboot' + '123456')
-- 2. 明文密码统一为：123456
-- 3. 老师端账号：admin
-- 4. 学生端账号：23201321、23201322、23201323、23201324

INSERT INTO sys_user
(id, username, password_hash, real_name, role, phone, email, status)
VALUES
(1, 'admin', 'a384380c440fb620eb080df5cbfcd0f0', '管理员教师', 'ADMIN', '13800000000', 'admin@platform.local', 1),
(2, '23201321', 'a384380c440fb620eb080df5cbfcd0f0', '学生23201321', 'STUDENT', '13800000021', '23201321@platform.local', 1),
(3, '23201322', 'a384380c440fb620eb080df5cbfcd0f0', '学生23201322', 'STUDENT', '13800000022', '23201322@platform.local', 1),
(4, '23201323', 'a384380c440fb620eb080df5cbfcd0f0', '学生23201323', 'STUDENT', '13800000023', '23201323@platform.local', 1),
(5, '23201324', 'a384380c440fb620eb080df5cbfcd0f0', '学生23201324', 'STUDENT', '13800000024', '23201324@platform.local', 1);

INSERT INTO teacher
(id, user_id, teacher_no, teacher_name, department, title)
VALUES
(1, 1, 'T20250001', '管理员教师', '软件工程教研室', '讲师');

INSERT INTO class_info
(id, class_name, major, grade_year)
VALUES
(1, '软件工程 2023 级 1 班', '软件工程', '2023');

INSERT INTO student
(id, user_id, student_no, student_name, gender, class_id)
VALUES
(1, 2, '23201321', '学生23201321', '男', 1),
(2, 3, '23201322', '学生23201322', '女', 1),
(3, 4, '23201323', '学生23201323', '男', 1),
(4, 5, '23201324', '学生23201324', '女', 1);

-- 9.2 课程与评分规则数据

INSERT INTO course
(id, course_name, course_code, semester, credit, total_hours, theory_hours, experiment_hours, teacher_id, class_id, status)
VALUES
(1, 'Web 高级编程', 'WEB-ADVANCED', '2025-2026-1', 3.0, 48, 32, 16, 1, 1, 'ACTIVE');

INSERT INTO assessment_item
(course_id, item_name, item_type, full_score, weight, source_type, sort_order)
VALUES
(1, '作业成绩', 'HOMEWORK', 30, 0.30, 'MANUAL', 1),
(1, '实验成绩', 'EXPERIMENT', 20, 0.20, 'EXPERIMENT_MODULE', 2),
(1, '大作业成绩', 'PROJECT', 50, 0.50, 'PROJECT_MODULE', 3);

INSERT INTO course_objective
(course_id, objective_code, objective_type, description, graduation_requirement, expected_rate, sort_order)
VALUES
(1, '课程目标1', '知识目标', '理解并掌握 Web 基础库、框架与工具的使用原理和方法。', '毕业要求5.1', 0.70, 1),
(1, '课程目标2', '能力目标', '能够运用 Web 基础库、框架与工具完成 Web 应用设计与开发。', '毕业要求5.1', 0.70, 2);

INSERT INTO objective_assessment_map
(course_id, objective_id, assessment_item_id, objective_score, description)
VALUES
(1, 1, 1, 30, '作业成绩支撑课程目标1'),
(1, 1, 3, 20, '大作业中的设计理解部分支撑课程目标1'),
(1, 2, 2, 20, '实验成绩支撑课程目标2'),
(1, 2, 3, 30, '大作业中的综合开发部分支撑课程目标2');

-- 9.3 学生课程成绩初始化数据

INSERT INTO course_student_score
(course_id, student_id, score_status)
VALUES
(1, 1, 'DRAFT'),
(1, 2, 'DRAFT'),
(1, 3, 'DRAFT'),
(1, 4, 'DRAFT');
