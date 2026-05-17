# 后端 Swagger / Knife4j 使用说明

本文档用于说明当前后端如何查看和使用接口文档，以及在联调阶段如何通过接口文档完成最基本的调试。

## 1. 当前接口文档方案

当前后端使用的是：

- Spring Boot 3
- OpenAPI 3
- Knife4j

对应依赖位于：`backend/pom.xml`

```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.5.0</version>
</dependency>
```

## 2. 访问地址

当前后端默认配置位于：`backend/src/main/resources/application.yml`

关键配置：

- 服务端口：`8101`
- 接口上下文：`/api`

因此本地启动后，接口文档地址通常为：

```text
http://localhost:8101/api/doc.html
```

如果需要直接查看 OpenAPI 原始 JSON，可以尝试：

```text
http://localhost:8101/api/v3/api-docs
```

## 3. 如何启动后端

在 `backend/` 目录下启动 Spring Boot：

```bash
mvn spring-boot:run
```

如果本机 Maven 默认没有绑定到 JDK 17+，需要先切换 `JAVA_HOME`。

当前项目已验证可使用：

```text
C:\Program Files\Java\jdk-21
```

## 4. 接口文档当前收录范围

当前 Knife4j 配置位于：`backend/src/main/resources/application.yml`

```yml
knife4j:
  enable: true
  openapi:
    title: "接口文档"
    version: 1.0
    group:
      default:
        api-rule: package
        api-rule-resources:
          - com.springboot.module
          - com.springboot.template.controller
```

这表示当前接口文档会收录：

- 新模块接口：`com.springboot.module.*`
- 历史模板控制器：`com.springboot.template.controller`

当前你最关心的认证接口主要来自：

- `com.springboot.module.auth.controller.UserController`
- `com.springboot.module.file.controller.FileController`

## 5. 当前认证接口

当前最基本登录注册后端接口如下：

### 5.1 学生注册

```text
POST /api/user/register
POST /api/user/register/student
```

请求体示例：

```json
{
  "username": "zhangsan01",
  "realName": "张三",
  "password": "123456",
  "checkPassword": "123456",
  "phone": "13800000000",
  "email": "test@example.com",
  "studentNo": "23201325",
  "gender": "男",
  "className": "软件工程 2023 级 1 班"
}
```

说明：

- 当前只开放学生注册
- 教师和管理员不开放注册
- `username` 和 `studentNo` 可以不同
- `className` 必须能在 `class_info` 表中精确匹配到现有班级

### 5.2 登录

```text
POST /api/user/login
```

请求体示例：

```json
{
  "username": "admin",
  "password": "123456"
}
```

返回示例：

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "管理员教师",
    "role": "ADMIN",
    "phone": "13800000000",
    "email": "admin@platform.local",
    "status": 1,
    "createdAt": "2026-05-17T00:00:00.000+00:00",
    "updatedAt": "2026-05-17T00:00:00.000+00:00"
  },
  "message": "ok"
}
```

### 5.3 获取当前登录用户

```text
GET /api/user/get/login
```

说明：

- 当前后端使用的是 `session` 登录态，不是 token
- 所以这个接口依赖浏览器或 Swagger 调试页面自动携带同一个会话的 cookie

### 5.4 退出登录

```text
POST /api/user/logout
```

## 6. 如何在 Swagger 页面里调试登录态接口

因为当前不是 token 模式，而是 session 模式，所以建议按下面顺序调试：

1. 先打开 `doc.html`
2. 调用 `POST /api/user/login`
3. 登录成功后，不要关闭当前页面
4. 在同一个 Swagger 页面中继续调用：
   - `GET /api/user/get/login`
   - `POST /api/user/logout`

这样浏览器会自动复用同一个 session cookie。

如果你登录成功后再调用 `get/login` 仍然提示未登录，优先检查：

1. 是否在同一个浏览器标签页中操作
2. 是否换了端口或域名
3. 是否被浏览器拦截了 cookie

## 7. 推荐调试顺序

建议后端联调时按下面顺序使用 Swagger：

1. 先测试学生注册
2. 再测试管理员或学生登录
3. 测试获取当前登录用户
4. 测试退出登录
5. 最后再测文件上传等需要登录上下文的接口

## 8. 常见问题

### 8.1 打不开 `doc.html`

优先检查：

1. 后端是否已成功启动
2. 访问地址是否带了上下文 `/api`
3. `knife4j.enable` 是否为 `true`

### 8.2 文档里看不到某个接口

优先检查：

1. 该控制器是否在 `com.springboot.module` 或 `com.springboot.template.controller` 下
2. 是否使用了 `@RestController`
3. 启动的是否是最新代码

### 8.3 注册时报“班级不存在”

原因：

- 当前学生注册要求 `className` 必须能匹配 `class_info.class_name`

解决方式：

1. 先执行 `sql/schema.sql` 初始化数据
2. 或者先在数据库中插入对应班级数据

### 8.4 登录时报账号或密码错误

优先检查：

1. 数据库是否已执行初始化 SQL
2. 输入的是否是 `username`
3. 密码是否为初始化值 `123456`

## 9. 当前建议

当前项目正处于后端基础能力搭建阶段，建议把 Knife4j 作为：

- 后端自测入口
- 前后端联调入口
- 团队快速理解接口的入口

在后续开发中，新增控制器时尽量保持：

- 明确的路径前缀
- 清晰的请求体结构
- 一致的返回体 `BaseResponse<T>`

这样 Swagger 页面会更容易使用，也更适合答辩展示。
