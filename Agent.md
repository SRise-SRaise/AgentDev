# 项目整体结构说明

本文件用于帮助项目成员和后续协作 AI 快速理解当前项目的目录划分、模块职责和后续开发边界。

## 1. 项目根目录

### `backend/`
Spring Boot 后端工程目录。

作用：
- 放置后端 Java 源码、资源文件、后端配置和测试代码
- 统一承载课程平台所有后端接口和业务逻辑
- 当前保留原有模板业务代码，后续逐步向新模块目录迁移

### `frontend/`
Vue 前端工程目录。

作用：
- 放置前端页面、路由、接口封装、状态管理和通用组件
- 当前已搭建为空项目壳，用于后续按模块补充真实页面和接口
- 前端按教师端、学生端、Python 实验、Vue 实验、大作业等边界拆分目录

### `files/`
项目文档、模板、样例文件目录。

作用：
- 存放需求文档、架构文档、数据库设计说明、接口说明
- 存放后续课程资料模板、报表模板、样例文件
- 不用于程序运行时上传文件存储

当前建议子目录：
- `requirements/`：需求说明、业务分析文档
- `architecture/`：总体架构、目录调整、开发说明文档
- `database/`：数据库设计说明、表结构分析文档
- `interface/`：页面原型、接口说明、交互设计文档
- `report-templates/`：导出报表模板、课程质量材料模板
- `samples/`：样例导入文件、示例压缩包、示例截图

### `storage/`
运行期文件目录。

作用：
- 存放系统运行中生成或上传的文件
- 开发环境先使用本地目录，后续可切换对象存储
- 默认不应提交真实业务文件到仓库

当前子目录说明：
- `syllabus/`：课程大纲导入文件
- `homework-score-import/`：作业成绩导入文件
- `python-submit/`：Python 实验提交代码
- `python-run-log/`：Python 判题运行日志
- `vue-submit/`：Vue 实验提交压缩包
- `vue-unzip/`：Vue 项目解压目录
- `vue-screenshot/`：Vue 页面运行截图
- `project-zip/`：大作业提交压缩包
- `project-unzip/`：大作业解压目录
- `agent-report/`：Agent 评测报告
- `report-export/`：成绩表和质量报告导出结果

### `sql/`
数据库脚本目录。

作用：
- `schema.sql`：建表脚本或当前主库结构脚本
- `init-data.sql`：初始化数据脚本
- `upgrade/`：后续数据库增量变更脚本

## 2. 后端目录说明

后端主代码位于：`backend/src/main/java/com/springboot/`

### 已有基础目录

#### `common/`
公共返回体、分页对象、错误码、通用工具入口等基础能力。

#### `config/`
Spring Boot、MyBatis、跨域、静态资源、第三方能力等配置。

#### `exception/`
全局异常、业务异常和异常处理工具。

#### `utils/`
通用工具类。

#### `ai/`
当前 AI 模型配置和工厂类。

#### `docker/`
当前 Docker 连接和容器操作能力。

#### `template/`
当前保留的历史模板业务代码区。

作用：
- 当前主要保留 `post` 相关示例代码，作为历史参考
- 已迁走的能力包括：用户认证、文件上传、微信登录相关目录
- 后续新业务优先写入 `module/`、`mapper/`、`model/` 的新结构中

### 新增目标目录

#### `module/`
后端业务模块主目录，后续真实开发的主要位置。

子模块说明：
- `module/auth/`：登录、注册、权限控制
- `module/course/`：课程闭环评价与成绩管理
- `module/pythonlab/`：Python 实验任务、测试用例、提交、判题、评分
- `module/vuelab/`：Vue 实验任务、运行配置、提交、截图、评分
- `module/projectwork/`：大作业、分组、提交、评测、评分
- `module/file/`：文件上传下载、文件元数据、存储路径管理
- `module/ai/`：面向业务模块的 Agent 工具、提示词和服务能力

当前已经完成迁移的子模块：
- `module/auth/`：用户登录、注册、当前登录用户、微信开放平台登录、公众号路由相关代码
- `module/file/`：文件上传控制器和文件上传业务枚举

#### `mapper/`
数据库访问层目录。

作用：
- 统一放置 MyBatis / MyBatis-Plus Mapper 接口
- 按业务领域分子目录，避免不同开发者修改同一层文件

建议子目录：
- `system/`
- `course/`
- `experiment/`
- `pythonlab/`
- `vuelab/`
- `projectwork/`
- `file/`

#### `model/entity/`
数据库实体对象目录。

作用：
- 与数据库表结构对应
- 公共表和扩展表统一管理，但按业务领域拆目录

#### `model/dto/`
请求对象目录。

作用：
- 接收前端请求参数
- 让 Python、Vue、课程闭环、大作业的请求结构彻底分开

当前已落位：
- `model/dto/auth/`：登录、注册、用户查询、用户更新等请求对象
- `model/dto/file/`：文件上传请求对象

#### `model/vo/`
响应对象目录。

作用：
- 定义返回前端的数据结构
- 避免直接把实体类暴露给前端

当前已落位：
- `model/vo/auth/`：登录用户和用户脱敏视图对象

### `backend/src/main/resources/`
后端资源目录。

作用：
- 放置 `application.yml` 等配置文件
- 放置 Mapper XML 文件
- 放置模板资源或其他静态资源

当前需要注意：
- 当前上传和静态文件访问已经切换为根目录 `storage/`
- `resources/files/` 目录如果仍然存在，可视为历史模板残留目录，不建议继续写入运行期文件

## 3. 前端目录说明

前端主代码位于：`frontend/src/`

### `router/`
路由配置目录。

作用：
- `index.js`：聚合教师端和学生端路由
- `teacher.routes.js`：教师端页面路由
- `student.routes.js`：学生端页面路由

### `api/`
前端接口封装目录。

作用：
- 每个业务模块单独维护自己的接口文件
- 避免 Python 实验和 Vue 实验共用同一份大而杂的接口文件

当前子目录：
- `auth/`
- `course/`
- `pythonlab/`
- `vuelab/`
- `projectwork/`
- `file/`

### `views/`
页面目录。

作用：
- 按身份和业务边界拆分页面
- 教师端、学生端页面分开
- Python 实验与 Vue 实验页面分开

结构说明：
- `views/auth/`：登录等认证页面
- `views/teacher/course/`：教师端课程闭环页面
- `views/teacher/experiment/pythonlab/`：教师端 Python 实验页面
- `views/teacher/experiment/vuelab/`：教师端 Vue 实验页面
- `views/teacher/projectwork/`：教师端大作业页面
- `views/student/course/`：学生端课程相关页面
- `views/student/experiment/pythonlab/`：学生端 Python 实验页面
- `views/student/experiment/vuelab/`：学生端 Vue 实验页面
- `views/student/projectwork/`：学生端大作业页面

### `components/`
组件目录。

作用：
- `base/`：基础通用组件
- `layout/`：布局组件
- `course/`、`pythonlab/`、`vuelab/`、`projectwork/`：业务模块组件

### `stores/`
Pinia 状态管理目录。

作用：
- 统一管理登录用户、课程信息、实验状态、大作业状态等全局数据

### `utils/`
前端工具目录。

作用：
- `request.js`：Axios 请求实例
- `file.js`：文件相关工具
- `format.js`：格式化工具

## 4. 当前迁移原则

本轮调整遵循以下原则：

1. 先把目录骨架搭好，保证团队分工边界清晰。
2. 暂时保留后端模板示例代码，不在本轮直接清理。
3. 前端先提供可运行的空项目壳，后续逐模块填充真实业务。
4. 文档目录和运行期目录分离，避免文件职责混乱。
5. 以后新增业务代码，优先进入新的模块化目录，不再继续堆到模板目录中。

## 5. 后续开发建议

1. 优先从 `module/course/`、`module/pythonlab/`、`module/vuelab/` 开始补业务骨架。
2. 尽快把上传路径从 `backend/src/main/resources/files/` 切到根目录 `storage/`。
3. 前端先联通登录、课程、Python 实验、Vue 实验四条主路径。
4. 数据库脚本后续按统一实验主表、扩展表、成绩表继续细化。
