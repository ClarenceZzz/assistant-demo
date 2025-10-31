# 项目规范
## 看板工作流
本项目使用看板工作流进行任务管理，请严格遵守以下规范:
- 项目背景和需求记录在 ./docs/PRD.md，不可修改。
- Kanban 数据记录在 `./kanban/` 目录（Markdown 格式）下。
- `./kanban/board.md` 负责维护整个项目的所有任务状态。
- `./kanban/task/` 目录下存放每个任务的详细描述（Markdown）, 每个任务一个文件，文件名为任务 ID，例如 `T1-1-setup-cleaning-framework.md`。任务文件内不但要记录任务描述，还要记录开发过程中遇到的问题与解决方案、BUG 记录、Review 意见等。

任务开始前请检查 ./kanban/ 目录：
- 如果不存在，请根据 PRD 或遵照用户本次的指示，创建初始看板和任务文件。
- 如果存在，请根据看板上的任务状态，继续推进任务，或遵照用户本次的指示进行工作。

### 看板状态文件（board.md）
状态包含：
* **Backlog**（需求池）
* **To Do**（当前周期待办）
* **In Progress**（开发中）
* **In Review**（代码审查）
* **Testing / QA**（QA 测试中）
* **Blocked**（阻塞）
* **Done**（验收通过）

任务状态定义（严格）:
* **同一个任务 ID 只能处于一种任务状态中**
* **In Progress**：除了完成任务外，还需包含单元测试（若适用）。
* **In Review**：PR 描述需写清修改点、运行方式、影响面、回归风险；至少 1 名审查者通过（由你切换身份扮演）。
* **Testing / QA**：QA 按测试用例执行，记录 BUG，BUG 分级（P0/P1/P2）。所有 P0 必须解决，P1 需评估。QA 完成后 QA 在卡片上写“QA Passed”（由你切换身份扮演）。
* **Done**：产品经理或维护者对接收准则（Acceptance Criteria）进行最终验收，若通过，移动卡片到 Done。（由你切换身份扮演）

### 任务文件（e.g. T1-1-setup-cleaning-framework.md）

### 每个任务卡必须包含以下内容：

* **Goal**（任务目标）
* **Subtasks**（子任务，定义了分步实现任务目标的过程，在执行过程中视情况，你可以创建更细粒度的子任务，但不用更新到任务文件中）
* **Developer**（开发者、任务复杂度等信息）
* **Acceptance Criteria**（任务验收标准）
* **Test Cases**（测试用例和用例描述，可能为空或不全面，在测试过程中视情况，增加测试用例更新到这里）
* **QA**（任务执行完成后，记录根据任务验收标准进行验收的结果）
* **Related Files / Design Docs**（任务关联的设计文档、看板文档或其他文档）
* **Dependencies**（前置任务项或依赖任务项，若未完成拒绝执行本次任务）
* **Notes & Updates**（任务状态更新日志、任务备注或 Review 意见）

## 编码规范

**不能在测试类中进行删表操作**

### 1. Git 工作流

采用基于功能分支（Feature Branch）的工作流。

-   **主分支**:
    -   `main`: 生产分支，始终保持稳定和可部署状态。只接受来自 `develop` 分支的合并。
    -   `develop`: 开发主分支，集成了所有已完成的功能。是新功能分支的起点。
-   **分支命名**:
    -   功能开发: `feat/<feature-name>` (e.g., `feat/query-rewriting`)
    -   Bug修复: `fix/<issue-name-or-id>` (e.g., `fix/pg-connection-leak`)
    -   重构: `refactor/<module-name>` (e.g., `refactor/prompt-builder-logic`)
    -   文档: `docs/<topic>` (e.g., `docs/update-api-spec`)
-   **提交信息 (Commit Message)**:
    -   我们遵循 **Conventional Commits** 规范。
    -   格式: `<type>(<scope>): <subject>`
    -   **Type**: `feat`, `fix`, `refactor`, `test`, `docs`, `style`, `chore`, `build`
    -   **Scope** (可选): 影响的模块，如 `retriever`, `prompt`, `llm`, `controller`
    -   **示例**:
        -   `feat(retriever): add support for hybrid search`
        -   `fix(llm): handle dashscope api rate limit gracefully`
        -   `test(prompt): add unit tests for persona-based prompt generation`

### 2. 编码规范

代码的清晰性、健壮性和一致性是我们的首要原则。
-   **语言与风格**:
    -   遵循 **Google Java Style Guide**。
-   **面向对象与设计模式**:
    -   **职责单一**: 代码结构应与 `agents.md` 中定义的代理职责保持一致（如果有的话）。每个 Service/Component/Repository 应只负责一项核心任务（如 `KnowledgeRetriever` 只负责检索，不负责构建Prompt）。
    -   **依赖注入**: 全面使用 Spring 的依赖注入（`@Autowired`, `@Service`, `@Component`），避免手动 `new` 对象。
    -   **接口驱动**: 为核心服务（如 `LLMClient`, `VectorStoreRepository`）定义接口，便于测试时进行 Mock。
-   **AI 相关规范**:
    -   **Prompt 管理**:
        -   **严禁**将 Prompt 模板硬编码在 Java 代码中。
        -   所有 Prompt 模板应存放在 `resources/prompts/` 目录下，按功能或角色命名（如 `persona_friendly_assistant.txt`）。
        -   使用模板引擎（如 `String.format` 或更专业的库）动态填充模板。
    -   **异常处理**:
        -   为外部 AI 服务（LLM, Embedding）的调用定义专门的业务异常，如 `LlmApiException`, `VectorStoreException`。
        -   在 Controller 层使用 `@ControllerAdvice` 统一捕获这些异常，并返回标准化的错误响应。

### 3. 测试规范

我们奉行“测试驱动开发”（TDD）的理念，确保代码的可靠性。
-   **测试框架**:
    -   **JUnit 5**: 作为主要的单元测试和集成测试框架。
    -   **Mockito**: 用于 Mock 依赖对象。
    -   **AssertJ**: 提供流式、可读性强的断言。
-   **测试目录与命名**:
    -   测试代码位于 `src/test/java`，包结构与主代码保持一致。
    -   测试类名以 `Test` 结尾 (e.g., `PromptBuilderTest.java`)。
    -   测试方法名清晰地描述测试场景 (e.g., `shouldGenerateCorrectPromptWhenPersonaIsProfessional()`)。
-   **测试类型**:
    1.  **单元测试 (Unit Tests)**:
        -   **目标**: 针对单一类或方法进行测试，**必须** Mock 所有外部依赖（数据库、LLM API、其他 Service）。
        -   **要求**: 每个 Service 和工具类都必须有单元测试。
        -   **实践**: 使用 `@Mock` 和 `@InjectMocks` 注解。
    2.  **集成测试 (Integration Tests)**:
        -   **目标**: 测试服务与外部依赖（如数据库）的真实交互。
        -   **要求**: 为每个 Controller Endpoint 和 Repository 编写集成测试。
        -   **实践**:
            -   使用 `@SpringBootTest` 注解启动一个完整的 Spring 上下文。
            -   使用 **Testcontainers** 启动一个临时的 Postgres + pgvector 容器，确保测试环境的隔离和一致性。
            -   使用 **WireMock** 来模拟外部 LLM API 的行为，避免真实调用和网络依赖。
-   **测试覆盖率**:
    -   使用 JaCoCo 插件衡量测试覆盖率。
    -   目标覆盖率 **不低于 80%**。每次提交 PR 时，覆盖率不得下降。

### 4. 依赖管理

-   使用 **Maven** 作为项目构建和依赖管理工具。
-   在 `pom.xml` 中统一管理所有依赖及其版本。

### 5. 代码审查流程 (Pull Request)

1.  **确保你开始工作时没有处在主分支（main、develop）上，否则拒绝执行任务**。
2.  完成开发和测试后，确保所有本地测试通过 (`mvn clean verify` 或 `gradle build`)。
3.  向 `develop` 分支发起一个 **Pull Request (PR)**。
4.  **提交或 PR 描述模板**:
    ```markdown
    ### 1. 本次变更解决了什么问题？
    (简要描述背景和目标，关联 Jira/GitHub Issue ID)

    ### 2. 本次变更做了什么？
    (分点说明主要改动)
    - 实现了查询重写中的指代消解功能
    - 优化了向量检索的 SQL 查询性能
    - ...

    ### 3. 如何进行测试？
    (提供测试步骤或 Postman/curl 请求示例)
    - 运行 `PromptBuilderTest.java`
    - 发送 POST 请求到 `/api/rag/query`，请求体见附件...

    ### 4. Checklist
    - [x] 我已阅读并遵循了 `CONTRIBUTING.md`。
    - [x] 我的代码遵循了项目的编码规范，并通过了 Checkstyle 检查。
    - [x] 我为新增或修改的代码编写了必要的单元测试和集成测试。
    - [x] 所有测试均已通过，且代码覆盖率达标。
    - [x] 我已更新了相关的 API 文档（如 Swagger/OpenAPI）。
    ```
5.  PR 必须获得 **至少一位** 其他团队成员的批准（Approve）。
6.  所有 CI/CD 管道（如 Jenkins, GitHub Actions）的构建、测试、代码扫描必须通过。
7.  审查通过后，使用 **Squash and Merge** 将 PR 合并到 `develop` 分支，保持主干历史的整洁。

### 6. 文档与配置规范

-   **API 文档**:
    -   使用 **Springdoc-openapi** (Swagger 3) 为所有 RESTful API 自动生成交互式文档。
    -   为所有 Controller、DTO 和字段添加 `@Schema` 和 `@Operation` 等注解，提供清晰的描述。
-   **代码文档**:
    -   为所有 `public` 的类和方法编写标准的 **JavaDoc**。
-   **配置文件**:
    -   使用 Spring Boot 的 `application.yml` (或 `.properties`) 进行配置。
    -   使用 Spring Cloud Config、Vault 或操作系统环境变量来管理生产环境的敏感配置。
    -   提供一个 `application-dev.yml` 示例，方便新成员快速启动项目。