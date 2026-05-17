# 项目技术架构说明

## 技术栈概览

| 项目 | 版本 | 说明 |
|------|------|------|
| Java | 17 | JDK 版本 |
| Spring Boot | 3.2.3 | 基础框架 |
| MyBatis Plus | 3.5.5 | ORM 框架（Spring Boot 3 专用） |
| Spring AI | 1.0.0-M6 | 大模型集成（OpenAI 兼容协议） |
| Docker Java Client | 3.4.0 | Docker 容器管理 |
| RabbitMQ | Spring Boot 内置 | 消息队列 |

## 项目结构

```
com.springboot
├── template/               ← 模板业务代码（用户/帖子/微信等）
│   ├── controller/         控制器（UserController, PostController 等）
│   ├── service/impl/       服务层（UserService, PostService 等）
│   ├── mapper/             数据访问层（UserMapper, PostMapper 等）
│   ├── model/
│   │   ├── entity/         实体类（User, Post, PostThumb, PostFavour）
│   │   ├── dto/            数据传输对象（按业务分子包：user/post/file 等）
│   │   ├── vo/             视图对象（LoginUserVO, UserVO, PostVO）
│   │   └── enums/          枚举（UserRoleEnum, FileUploadBizEnum）
│   ├── esdao/              Elasticsearch 数据访问
│   ├── job/                定时任务（同步帖子到 ES）
│   ├── wxmp/               微信公众号消息处理
│   └── generate/           MyBatis Plus 代码生成器
│
├── ai/                     ← Spring AI 模型工厂
│   ├── AiModelType.java    模型类型枚举（FAST/SMART/EMBEDDING）
│   ├── AiModelProperties.java  多模型配置属性
│   ├── AiModelConfig.java  多模型 Bean 注册
│   └── AiModelFactory.java 模型工厂（按类型获取模型实例）
│
├── docker/                 ← Docker 容器管理
│   ├── DockerProperties.java   Docker 连接配置属性
│   └── DockerManager.java  容器操作封装（创建/执行/销毁等）
│
├── common/                 通用响应封装（BaseResponse, ErrorCode, ResultUtils）
├── config/                 配置类（CORS, COS, JSON, MyBatisPlus, WebMvc, WxOpen）
├── annotation/             自定义注解（@AuthCheck）
├── aop/                    切面（权限拦截、日志拦截）
├── exception/              异常体系（BusinessException, GlobalExceptionHandler）
├── constant/               常量定义
├── utils/                  工具类
├── manager/                第三方服务管理层（CosManager）
└── MainApplication.java    启动入口
```

## Spring AI 模型工厂

### 设计思路

采用 OpenAI 兼容协议接入大模型，一套依赖可对接所有 OpenAI 兼容模型提供商（智谱、DeepSeek、月之暗面等），切换模型只需修改配置。

### 使用方式

```java
@Autowired
private AiModelFactory aiModelFactory;

// 按类型获取模型
ChatModel chatModel = aiModelFactory.getChatModel(AiModelType.FAST);
ChatResponse response = chatModel.call(new Prompt("你好"));

// 便捷方法
ChatModel fast = aiModelFactory.getFastChatModel();     // glm-4-flash
ChatModel smart = aiModelFactory.getSmartChatModel();    // glm-4-plus
EmbeddingModel embedding = aiModelFactory.getEmbeddingModel(); // embedding-2
```

### 模型类型

| 类型 | 枚举值 | 默认模型 | 说明 |
|------|--------|----------|------|
| 快速模型 | `AiModelType.FAST` | glm-4-flash | 日常对话，速度快成本低 |
| 强力模型 | `AiModelType.SMART` | glm-4-plus | 复杂任务，推理能力强 |
| 向量模型 | `AiModelType.EMBEDDING` | embedding-2 | 文本向量化 |

### 切换模型提供商

修改 `application.yml` 中的 `spring.ai.openai.base-url` 和 `api-key` 即可：

| 提供商 | base-url | model 示例 |
|--------|----------|------------|
| 智谱 | `https://open.bigmodel.cn/api/paas/v4` | glm-4-flash |
| DeepSeek | `https://api.deepseek.com/v1` | deepseek-chat |
| 月之暗面 | `https://api.moonshot.cn/v1` | moonshot-v1-8k |

### 自定义模型配置

在 `application.yml` 的 `ai.models` 下修改各模型的 model 名称和 temperature：

```yaml
ai:
  models:
    fast:
      model: glm-4-flash
      temperature: 0.7
    smart:
      model: glm-4-plus
      temperature: 0.5
```

## Docker 容器管理

### 设计思路

封装 Docker Java Client 为 `DockerManager` 工具类，提供容器全生命周期操作。用于后续代码沙箱功能（在容器中执行用户提交的 Python/Vue 项目代码）。

### 使用方式

```java
@Autowired
private DockerManager dockerManager;

// 创建并运行容器
String containerId = dockerManager.createContainer("python:3.11-slim", "python", "-c", "print('hello')");
dockerManager.startContainer(containerId);

// 获取执行日志
String log = dockerManager.getContainerLog(containerId);

// 在容器内执行命令
String output = dockerManager.executeCommand(containerId, "python", "-c", "print('world')");

// 销毁容器
dockerManager.removeContainer(containerId);
```

### 资源限制

```java
// 创建带资源限制的容器（256MB 内存，1 核 CPU，网络隔离）
String id = dockerManager.createContainerWithResourceLimits(
    "python:3.11-slim",
    256 * 1024 * 1024L,  // 内存限制 256MB
    100000L,              // CPU 周期
    100000L,              // CPU 配额（1 核）
    "python", "main.py"
);
```

### API 一览

| 方法 | 说明 |
|------|------|
| `createContainer(image, cmd)` | 创建容器 |
| `createContainerWithResourceLimits(image, memory, cpuPeriod, cpuQuota, cmd)` | 创建受限容器（内存/CPU/网络隔离） |
| `startContainer(id)` | 启动容器 |
| `stopContainer(id)` / `stopContainer(id, timeout)` | 停止容器 |
| `removeContainer(id)` | 强制移除容器及挂载卷 |
| `getContainerLog(id)` / `getContainerLog(id, timeout)` | 获取容器标准输出+错误日志 |
| `executeCommand(id, cmd)` | 在运行中的容器内执行命令 |
| `isContainerRunning(id)` | 检查容器运行状态 |
| `pullImage(image)` | 拉取镜像 |
| `listContainers()` | 列出所有容器 |
| `getClient()` | 获取原生 DockerClient（高级操作用） |

### Docker 连接配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `docker.host` | `unix:///var/run/docker.sock` | Docker 连接地址（Windows 用 `tcp://localhost:2375`） |
| `docker.tls-verify` | `false` | 是否启用 TLS |
| `docker.max-connections` | `100` | 最大连接数 |
| `docker.connection-timeout-seconds` | `30` | 连接超时 |
| `docker.response-timeout-seconds` | `120` | 响应超时 |

## RabbitMQ 消息队列

### 使用方式

直接使用 Spring AMQP 提供的 `RabbitTemplate` 和 `@RabbitListener`，无需额外封装。

```java
// 发送消息
@Autowired
private RabbitTemplate rabbitTemplate;
rabbitTemplate.convertAndSend("exchange", "routingKey", message);

// 接收消息
@RabbitListener(queues = "code.execute.queue")
public void handleMessage(String message) {
    // 处理消息
}
```

### 配置

RabbitMQ 配置默认注释状态，使用时取消注释并填入实际地址：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        concurrency: 5          # 最小消费者数
        max-concurrency: 10     # 最大消费者数
        prefetch: 1             # 预取数量
```

## 代码沙箱架构规划

后续将基于 DockerManager + RabbitMQ 实现代码沙箱，用于学校大作业评分系统：

```
学生提交代码 → RabbitMQ 队列 → Worker 取任务 → DockerManager 创建容器执行 → 返回结果 → 评分
```

**采用任务队列模式而非容器池**：
- 每次提交创建全新容器，天然隔离，无状态残留
- 通过限制 Worker 数量控制并发
- 实现简单，适合学校场景的并发量

## 完整依赖清单

| 依赖 | 版本 | 说明 |
|------|------|------|
| spring-boot-starter-web | 3.2.3 | Web 开发 |
| spring-boot-starter-aop | 3.2.3 | AOP 切面 |
| spring-boot-starter-freemarker | 3.2.3 | FreeMarker 模板引擎 |
| spring-boot-starter-data-redis | 3.2.3 | Redis 缓存（未启用） |
| spring-session-data-redis | 3.2.3 | 分布式 Session（未启用） |
| spring-boot-starter-data-elasticsearch | 3.2.3 | Elasticsearch 搜索（未启用） |
| spring-boot-starter-amqp | 3.2.3 | RabbitMQ 消息队列（未启用） |
| spring-ai-openai-spring-boot-starter | 1.0.0-M6 | Spring AI（OpenAI 兼容协议） |
| mybatis-plus-spring-boot3-starter | 3.5.5 | MyBatis Plus ORM |
| wx-java-mp-spring-boot-starter | 4.6.0 | 微信公众号 SDK |
| knife4j-openapi3-jakarta-spring-boot-starter | 4.5.0 | API 接口文档 |
| docker-java | 3.4.0 | Docker Java Client 核心 |
| docker-java-transport-httpclient5 | 3.4.0 | Docker HTTP 传输层 |
| cos_api | 5.6.89 | 腾讯云对象存储 |
| easyexcel | 3.1.1 | Excel 导入导出 |
| hutool-all | 5.8.8 | Hutool 工具集合 |
| lombok | 1.18.30 | 代码简化注解 |
