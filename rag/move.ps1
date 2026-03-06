$ErrorActionPreference = "Stop"
$baseDir = "d:\project\assistant-demo\rag\src\main\java\com\example\springaialibaba"

$structure = @(
    @{ Path = "config"; Files = "config\AdvisorHitlConfig.java", "config\ChatClientConfig.java", "config\JdbcConfig.java", "config\RestTemplateConfig.java", "tool\CustomToolConfig.java", "tool\ToolConfig.java" },
    @{ Path = "config\properties"; Files = "chat\generic\GenericChatProperties.java", "embedding\siliconflow\SiliconFlowEmbeddingProperties.java", "rerank\siliconflow\SiliconFlowRerankProperties.java", "prompt\PromptProperties.java" },
    @{ Path = "controller"; Files = "controller\ChatController.java", "controller\ChatHistoryController.java", "controller\RagController.java" },
    @{ Path = "service"; Files = "service\AgentService.java", "service\AdvisorHitlService.java", "service\ApprovalService.java", "chat\history\ChatHistoryService.java", "generation\GenerationService.java", "retrieval\RetrievalService.java" },
    @{ Path = "repository"; Files = "chat\history\ChatMessageRepository.java", "chat\history\ChatSessionRepository.java", "service\PendingApprovalStore.java" },
    @{ Path = "model\entity"; Files = "chat\history\ChatMessage.java", "chat\history\ChatSession.java", "service\PendingApproval.java", "rerank\RerankedDocument.java" },
    @{ Path = "model\enums"; Files = "chat\history\ChatMessageRole.java", "chat\history\ChatSessionStatus.java" },
    @{ Path = "model\dto"; Files = "controller\dto\ApiErrorResponse.java", "chat\generic\model\ChatCompletionRequest.java", "chat\generic\model\ChatCompletionResponse.java", "controller\dto\RagQueryRequest.java", "controller\dto\RagQueryResponse.java", "controller\dto\ReferenceDto.java", "tool\RunProgramRequest.java", "embedding\siliconflow\model\SiliconFlowEmbeddingRequest.java", "embedding\siliconflow\model\SiliconFlowEmbeddingResponse.java", "rerank\siliconflow\model\SiliconFlowRerankRequest.java", "rerank\siliconflow\model\SiliconFlowRerankResponse.java", "controller\dto\UpdateChatSessionRequest.java", "tool\WeatherRequest.java", "tool\WeatherResponse.java" },
    @{ Path = "exception"; Files = "controller\GlobalExceptionHandler.java", "chat\generic\GenericChatApiException.java", "embedding\siliconflow\SiliconFlowApiException.java", "rerank\siliconflow\SiliconFlowRerankException.java", "service\ToolApprovalRequiredException.java" },
    @{ Path = "core\advisor"; Files = "advisor\HumanInTheLoopAdvisor.java" },
    @{ Path = "core\client"; Files = "chat\generic\GenericChatClient.java", "rerank\siliconflow\RerankClient.java", "embedding\siliconflow\SiliconFlowEmbeddingClient.java" },
    @{ Path = "core\model"; Files = "embedding\DeterministicEmbeddingModel.java" },
    @{ Path = "core\tool"; Files = "tool\DateTool.java", "tool\RunProgramTool.java", "tool\WeatherTool.java" },
    @{ Path = "core\formatter"; Files = "formatter\DefaultResponseFormatter.java", "formatter\ResponseFormatter.java" },
    @{ Path = "core\preprocessor"; Files = "preprocessor\QueryPreprocessor.java" },
    @{ Path = "core\prompt"; Files = "prompt\DynamicPromptBuilder.java" }
)

foreach ($item in $structure) {
    $targetPath = Join-Path -Path $baseDir -ChildPath $item.Path
    if (-not (Test-Path -Path $targetPath)) {
        New-Item -ItemType Directory -Path $targetPath -Force | Out-Null
    }
    foreach ($file in $item.Files) {
        $sourcePath = Join-Path -Path $baseDir -ChildPath $file
        if (Test-Path -Path $sourcePath) {
            Write-Host "Moving $sourcePath to $targetPath"
            Move-Item -Path $sourcePath -Destination $targetPath -Force
        }
    }
}

# Remove empty directories
Get-ChildItem -Path $baseDir -Recurse -Directory | Where-Object { $_.GetFileSystemInfos().Count -eq 0 } | Remove-Item -Force -Recurse
Get-ChildItem -Path $baseDir -Recurse -Directory | Where-Object { $_.GetFileSystemInfos().Count -eq 0 } | Remove-Item -Force -Recurse

Write-Host "Move script completed."
