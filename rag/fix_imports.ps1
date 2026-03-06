$ErrorActionPreference = "Stop"
$srcDir = "d:\project\assistant-demo\rag\src"
$bp = "com.example.springaialibaba"

# 精确的旧类全限定名 → 新类全限定名 映射
$classMappings = @{
    # chat.history -> model.entity / model.enums / service / repository
    "$bp.chat.history.ChatMessage"              = "$bp.model.entity.ChatMessage"
    "$bp.chat.history.ChatSession"              = "$bp.model.entity.ChatSession"
    "$bp.chat.history.ChatMessageRole"          = "$bp.model.enums.ChatMessageRole"
    "$bp.chat.history.ChatSessionStatus"        = "$bp.model.enums.ChatSessionStatus"
    "$bp.chat.history.ChatHistoryService"       = "$bp.service.ChatHistoryService"
    "$bp.chat.history.ChatMessageRepository"    = "$bp.repository.ChatMessageRepository"
    "$bp.chat.history.ChatSessionRepository"    = "$bp.repository.ChatSessionRepository"

    # generation -> core.rag
    "$bp.generation.GenerationService"          = "$bp.core.rag.GenerationService"

    # retrieval -> core.rag
    "$bp.retrieval.RetrievalService"            = "$bp.core.rag.RetrievalService"

    # rerank -> model.entity
    "$bp.rerank.RerankedDocument"               = "$bp.model.entity.RerankedDocument"

    # rerank.siliconflow -> core.client
    "$bp.rerank.siliconflow.RerankClient"       = "$bp.core.client.RerankClient"

    # rerank.siliconflow.model -> model.dto
    "$bp.rerank.siliconflow.model.SiliconFlowRerankRequest"         = "$bp.model.dto.SiliconFlowRerankRequest"
    "$bp.rerank.siliconflow.model.SiliconFlowRerankResponse"        = "$bp.model.dto.SiliconFlowRerankResponse"
    "$bp.rerank.siliconflow.model.SiliconFlowRerankResponse.Result" = "$bp.model.dto.SiliconFlowRerankResponse.Result"

    # embedding.siliconflow -> core.client
    "$bp.embedding.siliconflow.SiliconFlowEmbeddingClient"          = "$bp.core.client.SiliconFlowEmbeddingClient"

    # embedding.siliconflow.model -> model.dto
    "$bp.embedding.siliconflow.model.SiliconFlowEmbeddingRequest"                 = "$bp.model.dto.SiliconFlowEmbeddingRequest"
    "$bp.embedding.siliconflow.model.SiliconFlowEmbeddingResponse"                = "$bp.model.dto.SiliconFlowEmbeddingResponse"
    "$bp.embedding.siliconflow.model.SiliconFlowEmbeddingResponse.EmbeddingData"  = "$bp.model.dto.SiliconFlowEmbeddingResponse.EmbeddingData"
    "$bp.embedding.siliconflow.model.SiliconFlowEmbeddingResponse.Usage"          = "$bp.model.dto.SiliconFlowEmbeddingResponse.Usage"

    # chat.generic -> core.client / exception / model.dto
    "$bp.chat.generic.GenericChatClient"                            = "$bp.core.client.GenericChatClient"
    "$bp.chat.generic.GenericChatApiException"                      = "$bp.exception.GenericChatApiException"
    "$bp.chat.generic.model.ChatCompletionRequest"                  = "$bp.model.dto.ChatCompletionRequest"
    "$bp.chat.generic.model.ChatCompletionRequest.ChatCompletionMessage" = "$bp.model.dto.ChatCompletionRequest.ChatCompletionMessage"
    "$bp.chat.generic.model.ChatCompletionResponse"                 = "$bp.model.dto.ChatCompletionResponse"

    # formatter -> core.formatter
    "$bp.formatter.ResponseFormatter"           = "$bp.core.formatter.ResponseFormatter"
    "$bp.formatter.DefaultResponseFormatter"    = "$bp.core.formatter.DefaultResponseFormatter"

    # preprocessor -> core.preprocessor
    "$bp.preprocessor.QueryPreprocessor"        = "$bp.core.preprocessor.QueryPreprocessor"

    # prompt -> core.prompt
    "$bp.prompt.DynamicPromptBuilder"           = "$bp.core.prompt.DynamicPromptBuilder"

    # tool -> model.dto / core.tool
    "$bp.tool.WeatherTool"                      = "$bp.core.tool.WeatherTool"
    "$bp.tool.DateTool"                         = "$bp.core.tool.DateTool"
    "$bp.tool.RunProgramTool"                   = "$bp.core.tool.RunProgramTool"
    "$bp.tool.WeatherRequest"                   = "$bp.model.dto.WeatherRequest"
    "$bp.tool.WeatherResponse"                  = "$bp.model.dto.WeatherResponse"
    "$bp.tool.RunProgramRequest"                = "$bp.model.dto.RunProgramRequest"

    # advisor -> core.advisor
    "$bp.advisor.HumanInTheLoopAdvisor"         = "$bp.core.advisor.HumanInTheLoopAdvisor"

    # embedding -> core.model
    "$bp.embedding.DeterministicEmbeddingModel" = "$bp.core.model.DeterministicEmbeddingModel"

    # service.PendingApproval -> model.entity
    "$bp.service.PendingApproval"               = "$bp.model.entity.PendingApproval"

    # service.ToolApprovalRequiredException -> exception
    "$bp.service.ToolApprovalRequiredException" = "$bp.exception.ToolApprovalRequiredException"

    # controller.dto -> model.dto
    "$bp.controller.dto.RagQueryRequest"        = "$bp.model.dto.RagQueryRequest"
    "$bp.controller.dto.RagQueryResponse"       = "$bp.model.dto.RagQueryResponse"
    "$bp.controller.dto.ReferenceDto"           = "$bp.model.dto.ReferenceDto"
    "$bp.controller.dto.ApiErrorResponse"       = "$bp.model.dto.ApiErrorResponse"
    "$bp.controller.dto.UpdateChatSessionRequest" = "$bp.model.dto.UpdateChatSessionRequest"

    # config.properties
    "$bp.chat.generic.GenericChatProperties"                    = "$bp.config.properties.GenericChatProperties"
    "$bp.embedding.siliconflow.SiliconFlowEmbeddingProperties"  = "$bp.config.properties.SiliconFlowEmbeddingProperties"
    "$bp.rerank.siliconflow.SiliconFlowRerankProperties"        = "$bp.config.properties.SiliconFlowRerankProperties"
    "$bp.prompt.PromptProperties"                               = "$bp.config.properties.PromptProperties"

    # exception
    "$bp.rerank.siliconflow.SiliconFlowRerankException"         = "$bp.exception.SiliconFlowRerankException"
    "$bp.embedding.siliconflow.SiliconFlowApiException"         = "$bp.exception.SiliconFlowApiException"
}

$allJavaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java"
$fixedCount = 0

foreach ($file in $allJavaFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $newContent = $content

    foreach ($oldFqn in ($classMappings.Keys | Sort-Object -Descending)) {
        $newFqn = $classMappings[$oldFqn]
        # 替换 import 语句
        $newContent = $newContent -replace "import\s+$([Regex]::Escape($oldFqn));", "import $newFqn;"
    }

    if ($newContent -ne $content) {
        [System.IO.File]::WriteAllText($file.FullName, $newContent, [System.Text.UTF8Encoding]::new($false))
        Write-Host "[FIXED] $($file.Name)"
        $fixedCount++
    }
}

Write-Host ""
Write-Host "=== 共修复 $fixedCount 个文件 ==="
