# T1-6: 修复 SpringAiApplication 启动与配置错误

## Goal
解决 `SpringAiApplication` 启动时的 `HttpMessageNotWritableException` 和 Netty 相关错误，确保应用在 Servlet 容器下正常运行并支持流式对话。

## Subtasks
- [ ] 移除 `pom.xml` 中的 `spring-boot-starter-webflux` 依赖。
- [ ] 重构 `ChatModelController`，使用 `SseEmitter` 替代 `Flux` 进行流式响应。
- [ ] 增加错误处理逻辑，确保 API 调用失败时返回清晰的错误信息。
- [ ] 验证 `/chat` 和 `/stream/chat` 接口的可用性。

## Developer
Antigravity

## Acceptance Criteria
- 应用启动无报错。
- 访问 `/chat` 接口能正常返回 AI 响应。
- 访问 `/stream/chat` 接口能正常流式返回 AI 响应。
- 无 `HttpMessageNotWritableException` 异常日志。

## Related Files / Design Docs
- `springai/pom.xml`
- `springai/src/main/java/com/example/springai/controller/ChatModelController.java`
- `implementation_plan.md`

## Notes & Updates
- 2026-02-20: 初始创建。发现 WebFlux 与 Web Starter 冲突，决定移除 WebFlux。
