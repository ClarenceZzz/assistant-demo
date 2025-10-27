# Spring AI Alibaba 脚手架项目

一个基于 Spring Boot 与 Spring AI DashScope Starter 的示例工程，提供最小可运行的阿里云通义千问（DashScope）调用骨架，便于快速接入聊天大模型。

## 快速开始

### 1. 准备环境
- JDK 17+
- Maven 3.9+
- DashScope API Key（在环境变量 `DASHSCOPE_API_KEY` 中配置）

### 2. 安装依赖

```bash
make setup
```

### 3. 本地运行

```bash
export DASHSCOPE_API_KEY=你的Key
make run
```

服务默认监听 `http://localhost:8080`，可通过如下请求体验聊天接口：

```bash
curl -X POST \
  http://localhost:8080/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"用一句话介绍Spring AI"}'
```

### 4. 测试与质量校验

```bash
make test   # 运行单元测试
make lint   # 执行 Maven verify（跳过测试）
```

- 默认情况下，`PgVectorStoreIntegrationTest` 会自动检测 Docker。若未安装 Docker，该用例会被自动跳过；
  - 如需强制依赖 Docker 容器运行，请设置 `-Dpgvector.test.mode=container`；
  - 若本地已准备好带 pgvector 扩展的 PostgreSQL，可设置 `-Dpgvector.test.mode=external`（或 `PGVECTOR_TEST_MODE=external`）使用外部数据库；
  - 设置为 `skip` 可显式跳过该测试：`-Dpgvector.test.mode=skip`。

### 5. 日志输出

- 应用默认使用 Logback，将日志写入 `logs/spring-ai-alibaba-demo.log`，并按天/50MB 滚动备份至 `logs/archive/`。
- 可通过环境变量 `LOG_HOME` 自定义日志目录，例如：

```bash
LOG_HOME=/var/log/spring-ai-alibaba make run
```

## 项目结构

```
.
├── Makefile
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/springaialibaba
│   │   │   ├── SpringAiAlibabaApplication.java
│   │   │   └── controller/ChatController.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback-spring.xml
│   └── test
│       └── java/com/example/springaialibaba/SpringAiAlibabaApplicationTests.java
```

## 后续扩展建议
- 自定义 `ChatClient` 调用参数，支持多轮对话、函数调用等高级能力。
- 结合 `Spring Boot Actuator` 监控 AI 服务调用情况。
- 在 `tests/` 下补充集成测试以覆盖关键业务流程。
