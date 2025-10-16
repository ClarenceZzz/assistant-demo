# Repository Guidelines

## Project Structure & Module Organization
Keep documentation (this file, `README.md`) in the root. Application code lives under `src/main/java/com/example/springaialibaba/`, configuration in `src/main/resources/`, and tests under `src/test/java/`. Place web endpoints in `controller/`, shared configuration (for example `ChatClientConfig.java`) in `config/`, and keep environment-dependent YAML or Logback updates in `application.yml` and `logback-spring.xml`. Generated or runtime artifacts such as `logs/` and `target/` stay out of version control.

## Build, Test, and Development Commands
`make setup` primes Maven dependencies (`mvn -B dependency:go-offline`).  
`make lint` runs `mvn -B -DskipTests verify` for compilation and static analysis.  
`make test` executes the JUnit test suite (`mvn test`).  
`make run` launches the Spring Boot app via `mvn spring-boot:run`.  
Set `DASHSCOPE_API_KEY` (and optionally `LOG_HOME`) before running to enable DashScope access and custom log directories.

## Coding Style & Naming Conventions
Target Java 17 syntax, keep 4-space indentation, and rely on IDE or Maven formatting to avoid wildcard imports. Packages remain lowercase (`com.example.springaialibaba`), classes use PascalCase (`ChatController`), methods and fields use lowerCamelCase, and constants are SCREAMING_SNAKE_CASE. Use SLF4J (`LoggerFactory`) for logging; avoid `System.out` in production paths. New configuration beans should live under `config/` to maintain discoverability.

## Testing Guidelines
Write unit and slice tests with JUnit 5 under `src/test/java`, mirroring package paths (`SpringAiAlibabaApplicationTests` covers context loads). Run `make test` locally before pushing. If adding integration coverage, tag slow or external DashScope calls with `@Tag("slow")` so they can be excluded via `mvn test -Dgroups=!slow`. Aim to leave the suite deterministic; mock external services where possible.

## Commit & Pull Request Guidelines
Follow the existing history: short, imperative commit subjects with optional prefixes (`feat: 引入 Logback 日志配置`). Each PR should describe the problem, summarize the solution, note verification (`make test`, manual curl, etc.), and link issues (e.g., `Fixes #123`). Include log excerpts or screenshots when altering CLI or API behaviour. Ensure CI passes before requesting review and call out follow-up work if any.

## Security & Configuration Tips
Never commit real DashScope keys. Externalize secrets via environment variables or `.env` (already ignored). Keep dependency versions pinned in `pom.xml` and review transitive upgrades. Default logging writes to `logs/spring-ai-alibaba-demo.log`; rotate or relocate via `LOG_HOME` in production environments. For additional configuration, prefer Spring Boot profiles over hard-coded values.
