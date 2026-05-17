# SpringBoot 模板开发指导文档

## 一、项目概述

本项目是一个基于 SpringBoot 3.2.3 + Java 17 的后端服务模板，提供了一套完整的企业级开发架构，包含用户管理、帖子管理、文件上传、微信集成等功能。

### 基础信息
- **Spring Boot 版本**: 3.2.3
- **Java 版本**: 17
- **服务端口**: 8101
- **接口前缀**: `/api`
- **模板引擎**: FreeMarker

---

## 二、技术栈与组件库

### 2.1 核心框架

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.3 | 核心框架 |
| Spring Boot Starter Web | 内置 | Web 开发 |
| Spring Boot Starter AOP | 内置 | AOP 切面编程 |
| Spring Boot Starter Test | 内置 | 单元测试 |

### 2.2 数据层

| 组件 | 版本 | 用途 |
|------|------|------|
| MyBatis Plus | 3.5.5 | ORM 框架 |
| MySQL Connector | 内置 | MySQL 驱动 |

### 2.3 缓存与搜索

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Data Redis | 内置 | Redis 缓存 |
| Spring Session Data Redis | 内置 | 分布式 Session |
| Spring Data Elasticsearch | 内置 | ES 搜索集成 |

### 2.4 第三方集成

| 组件 | 版本 | 用途 |
|------|------|------|
| WxJava MP | 4.6.0 | 微信公众号开发 |
| Knife4j OpenAPI | 4.5.0 | API 接口文档 |
| Tencent COS | 5.6.89 | 腾讯云对象存储 |
| EasyExcel | 3.1.1 | Excel 导入导出 |
| Hutool | 5.8.8 | 工具集合 |

### 2.5 其他

| 组件 | 版本 | 用途 |
|------|------|------|
| Lombok | 1.18.30 | 代码简化 |
| Apache Commons Lang3 | 内置 | 字符串工具 |

---

## 三、项目结构

```
src/
├── main/
│   ├── java/com/springboot/
│   │   ├── MainApplication.java          # 启动类
│   │   ├── annotation/                   # 自定义注解
│   │   │   └── AuthCheck.java            # 权限校验注解
│   │   ├── aop/                          # AOP 切面
│   │   │   ├── AuthInterceptor.java      # 权限拦截器
│   │   │   └── LogInterceptor.java       # 日志拦截器
│   │   ├── common/                       # 通用类
│   │   │   ├── BaseResponse.java         # 统一响应
│   │   │   ├── ResultUtils.java          # 响应工具
│   │   │   ├── ErrorCode.java            # 错误码枚举
│   │   │   ├── PageRequest.java          # 分页请求
│   │   │   └── DeleteRequest.java        # 删除请求
│   │   ├── config/                       # 配置类
│   │   │   ├── MyBatisPlusConfig.java    # MyBatis Plus 配置
│   │   │   ├── CorsConfig.java           # 跨域配置
│   │   │   ├── CosClientConfig.java      # COS 客户端配置
│   │   │   ├── JsonConfig.java           # JSON 配置
│   │   │   └── WxOpenConfig.java         # 微信配置
│   │   ├── constant/                     # 常量定义
│   │   │   ├── UserConstant.java         # 用户常量
│   │   │   ├── FileConstant.java         # 文件常量
│   │   │   └── CommonConstant.java      # 通用常量
│   │   ├── controller/                   # 控制器
│   │   │   ├── UserController.java       # 用户接口
│   │   │   ├── PostController.java       # 帖子接口
│   │   │   ├── FileController.java       # 文件接口
│   │   │   └── WxMpController.java      # 微信接口
│   │   ├── service/                      # 服务层
│   │   │   ├── UserService.java
│   │   │   ├── PostService.java
│   │   │   └── impl/                     # 服务实现
│   │   ├── mapper/                       # 数据访问层
│   │   ├── model/                        # 数据模型
│   │   │   ├── entity/                   # 实体类
│   │   │   ├── dto/                      # 数据传输对象
│   │   │   ├── vo/                       # 视图对象
│   │   │   └── enums/                    # 枚举
│   │   ├── exception/                    # 异常处理
│   │   │   ├── BusinessException.java   # 业务异常
│   │   │   ├── GlobalExceptionHandler.java # 全局异常处理
│   │   │   └── ThrowUtils.java           # 异常抛出工具
│   │   ├── manager/                      # 管理层
│   │   │   └── CosManager.java           # COS 文件管理
│   │   ├── utils/                        # 工具类
│   │   ├── esdao/                        # ES 数据访问
│   │   ├── wxmp/                         # 微信公众号
│   │   ├── job/                          # 定时任务
│   │   │   ├── once/                    # 单次任务
│   │   │   └── cycle/                   # 循环任务
│   │   └── generate/                     # 代码生成
│   │       └── CodeGenerator.java
│   └── resources/
│       ├── application.yml               # 主配置
│       ├── application-dev.yml           # 开发环境
│       ├── application-prod.yml          # 生产环境
│       ├── application-test.yml          # 测试环境
│       ├── mapper/                       # MyBatis XML
│       └── templates/                    # 代码生成模板
└── test/                                 # 测试类
```

---

## 四、已有功能

### 4.1 用户模块

| 功能 | 说明 | 路径 |
|------|------|------|
| 用户注册 | 账号密码注册 | `POST /api/user/register` |
| 用户登录 | 账号密码登录 | `POST /api/user/login` |
| 微信登录 | 微信开放平台登录 | `GET /api/user/login/wx_open` |
| 用户注销 | 退出登录 | `POST /api/user/logout` |
| 获取登录用户 | 获取当前用户信息 | `GET /api/user/get/login` |
| 创建用户 | 管理员操作 | `POST /api/user/add` |
| 删除用户 | 管理员操作 | `POST /api/user/delete` |
| 更新用户 | 管理员操作 | `POST /api/user/update` |
| 获取用户 | 根据ID获取 | `GET /api/user/get` |
| 分页查询 | 管理员查询 | `POST /api/user/list/page` |
| 分页查询VO | 封装类分页 | `POST /api/user/list/page/vo` |
| 更新个人信息 | 用户修改自身信息 | `POST /api/user/update/my` |

### 4.2 帖子模块

| 功能 | 说明 | 路径 |
|------|------|------|
| 创建帖子 | 用户发布帖子 | `POST /api/post/add` |
| 删除帖子 | 用户/管理员删除 | `POST /api/post/delete` |
| 更新帖子 | 仅管理员 | `POST /api/post/update` |
| 获取帖子 | 根据ID获取 | `GET /api/post/get/vo` |
| 分页查询 | 管理员查询 | `POST /api/post/list/page` |
| 分页查询VO | 封装类分页 | `POST /api/post/list/page/vo` |
| 我的帖子 | 当前用户帖子 | `POST /api/post/my/list/page/vo` |
| ES搜索 | 全文搜索 | `POST /api/post/search/page/vo` |
| 编辑帖子 | 用户编辑自己帖子 | `POST /api/post/edit` |

### 4.3 文件模块

| 功能 | 说明 | 路径 |
|------|------|------|
| 文件上传 | 上传到COS | `POST /api/file/upload` |

### 4.4 微信模块

| 功能 | 说明 | 路径 |
|------|------|------|
| 接收消息 | 微信公众号消息 | `POST /api/` |
| 验证服务器 | 微信服务器验证 | `GET /api/` |
| 设置菜单 | 设置公众号菜单 | `GET /api/setMenu` |

### 4.5 点赞收藏模块

- **帖子点赞**: `PostThumbController`
- **帖子收藏**: `PostFavourController`

---

## 五、配置指南

### 5.1 数据库配置

修改 `application.yml`:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: 123456
```

### 5.2 Redis 配置

取消注释并修改:

```yaml
spring:
  redis:
    database: 1
    host: localhost
    port: 6379
    timeout: 5000
    password: 123456
```

### 5.3 启用分布式 Session

在 `application.yml` 中取消注释:

```yaml
spring:
  session:
    store-type: redis
```

同时在 `MainApplication.java` 中移除 `RedisAutoConfiguration`:

```java
@SpringBootApplication
// @SpringBootApplication(exclude = {RedisAutoConfiguration.class})
public class MainApplication
```

### 5.4 Elasticsearch 配置

取消注释:

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: root
    password: 123456
```

### 5.5 微信公众号配置

修改 `application.yml`:

```yaml
wx:
  mp:
    token: xxx
    aesKey: xxx
    appId: xxx
    secret: xxx
```

### 5.6 腾讯云 COS 配置

修改 `application.yml`:

```yaml
cos:
  client:
    accessKey: xxx
    secretKey: xxx
    region: xxx
    bucket: xxx
```

### 5.7 接口文档

访问地址: `http://localhost:8101/doc.html`

---

## 六、开发规范

### 6.1 分层结构

- **Controller**: 负责接收请求、参数校验、返回响应
- **Service**: 负责业务逻辑处理
- **Mapper**: 负责数据库操作
- **Manager**: 负责第三方服务调用
- **Model**: 负责数据模型（Entity/DTO/VO）

### 6.2 命名规范

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| Controller | `XxxController` | `UserController` |
| Service 接口 | `XxxService` | `UserService` |
| Service 实现 | `XxxServiceImpl` | `UserServiceImpl` |
| Mapper | `XxxMapper` | `UserMapper` |
| Entity | `Xxx` | `User` |
| DTO | `XxxRequest/Response` | `UserRegisterRequest` |
| VO | `XxxVO` | `UserVO` |
| 枚举 | `XxxEnum` | `UserRoleEnum` |

### 6.3 API 返回格式

使用统一返回格式:

```java
// 成功
ResultUtils.success(data)

// 失败
ResultUtils.error(ErrorCode.PARAMS_ERROR)
throw new BusinessException(ErrorCode.PARAMS_ERROR, "错误信息")
```

返回结构:
```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

### 6.4 权限校验

使用 `@AuthCheck` 注解:

```java
// 需要管理员权限
@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
public BaseResponse<?> adminOnly() { ... }
```

### 6.5 错误码规范

在 `ErrorCode` 枚举中添加:

```java
SUCCESS(0, "ok"),
PARAMS_ERROR(40000, "请求参数错误"),
NOT_LOGIN_ERROR(40100, "未登录"),
NO_AUTH_ERROR(40101, "无权限"),
// 新增错误码
NEW_ERROR(40001, "新错误信息");
```

---

## 七、新增功能步骤

### 7.1 实体类定义

位置: `src/main/java/com/springboot/model/entity/`

```java
@TableName(value = "xxx")
@Data
public class Xxx implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String name;
    
    @TableLogic
    private Integer isDelete;
    
    private Date createTime;
    private Date updateTime;
}
```

### 7.2 使用代码生成器

1. 修改 `CodeGenerator.java` 中的参数:
   - `packageName`: 包名
   - `dataName`: 数据名称
   - `dataKey`: 业务键（小写）
   - `upperDataKey`: 业务键（大写）

2. 运行 `CodeGenerator.main()`

3. 生成的文件在 `generator/` 目录

### 7.3 手动创建流程

1. **创建 Entity**: `model/entity/Xxx.java`
2. **创建 Mapper**: `mapper/XxxMapper.java` + `mapper/XxxMapper.xml`
3. **创建 Service**: `service/XxxService.java` + `service/impl/XxxServiceImpl.java`
4. **创建 DTO**: `model/dto/xxx/XxxAddRequest.java` 等
5. **创建 VO**: `model/vo/XxxVO.java`
6. **创建 Controller**: `controller/XxxController.java`
7. **添加路由**: 参考现有 Controller 实现

---

## 八、常用工具类

### 8.1 ResultUtils - 返回工具

```java
// 成功返回
ResultUtils.success(data)

// 错误返回
ResultUtils.error(ErrorCode.PARAMS_ERROR)
ResultUtils.error(40000, "自定义错误信息")
```

### 8.2 ThrowUtils - 断言工具

```java
// 条件为真时抛出异常
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR)
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR, "自定义信息")
```

### 8.3 BusinessException - 业务异常

```java
throw new BusinessException(ErrorCode.PARAMS_ERROR)
throw new BusinessException(ErrorCode.PARAMS_ERROR, "自定义信息")
throw new BusinessException(40000, "自定义信息")
```

### 8.4 Hutool 常用工具

```java
// JSON 处理
JSONUtil.toJsonStr(obj)
JSONUtil.toBean(json, clazz)

// 集合处理
CollUtil.isEmpty(list)
CollUtil.isNotEmpty(list)

// 文件处理
FileUtil.exist(path)
FileUtil.getSuffix(filename)
```

---

## 九、注意事项

1. **逻辑删除**: 实体类中使用 `@TableLogic` 注解，实现软删除
2. **分布式锁**: 用户注册等关键操作使用 `synchronized` 锁定账户
3. **密码加密**: 使用 MD5 + 盐值加密
4. **分页限制**: 每页最大 20 条，防止爬虫
5. **接口文档**: 开发阶段启用 Knife4j，生产环境可关闭
6. **跨域配置**: 已配置 CORS，支持前后端分离开发

---

## 十、启动项目

```bash
# 使用 IDE 启动
# 或命令行启动
mvn spring-boot:run

# 访问接口文档
http://localhost:8101/doc.html
```