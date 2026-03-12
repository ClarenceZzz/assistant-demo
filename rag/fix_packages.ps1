$ErrorActionPreference = "Stop"
$baseDir = "d:\project\assistant-demo\rag\src\main\java\com\example\springaialibaba"
$basePackage = "com.example.springaialibaba"

# ======================================================
# Step 1: 定义「物理路径相对于 springaialibaba/」→ 「正确 package」的映射
# key = 文件相对路径（相对于 springaialibaba\），value = 正确 package
# ======================================================
$packageMap = @{
    "config\AdvisorHitlConfig.java"                         = "$basePackage.config"
    "config\ChatClientConfig.java"                           = "$basePackage.config"
    "config\JdbcConfig.java"                                 = "$basePackage.config"
    "config\RestTemplateConfig.java"                         = "$basePackage.config"
    "config\CustomToolConfig.java"                           = "$basePackage.config"
    "config\ToolConfig.java"                                 = "$basePackage.config"
    "config\properties\GenericChatProperties.java"           = "$basePackage.config.properties"
    "config\properties\SiliconFlowEmbeddingProperties.java"  = "$basePackage.config.properties"
    "config\properties\SiliconFlowRerankProperties.java"     = "$basePackage.config.properties"
    "config\properties\PromptProperties.java"                = "$basePackage.config.properties"
    "controller\ChatController.java"                         = "$basePackage.controller"
    "controller\ChatHistoryController.java"                  = "$basePackage.controller"
    "controller\RagController.java"                          = "$basePackage.controller"
    "service\AgentService.java"                              = "$basePackage.service"
    "service\AdvisorHitlService.java"                        = "$basePackage.service"
    "service\ApprovalService.java"                           = "$basePackage.service"
    "service\ChatHistoryService.java"                        = "$basePackage.service"
    "service\GenerationService.java"                         = "$basePackage.core.rag"
    "service\RetrievalService.java"                          = "$basePackage.core.rag"
    "repository\ChatMessageRepository.java"                  = "$basePackage.repository"
    "repository\ChatSessionRepository.java"                  = "$basePackage.repository"
    "repository\PendingApprovalStore.java"                   = "$basePackage.service"
    "model\entity\ChatMessage.java"                          = "$basePackage.model.entity"
    "model\entity\ChatSession.java"                          = "$basePackage.model.entity"
    "model\entity\PendingApproval.java"                      = "$basePackage.model.entity"
    "model\entity\RerankedDocument.java"                     = "$basePackage.model.entity"
    "model\enums\ChatMessageRole.java"                       = "$basePackage.model.enums"
    "model\enums\ChatSessionStatus.java"                     = "$basePackage.model.enums"
    "model\dto\ApiErrorResponse.java"                        = "$basePackage.model.dto"
    "model\dto\ChatCompletionRequest.java"                   = "$basePackage.model.dto"
    "model\dto\ChatCompletionResponse.java"                  = "$basePackage.model.dto"
    "model\dto\RagQueryRequest.java"                         = "$basePackage.model.dto"
    "model\dto\RagQueryResponse.java"                        = "$basePackage.model.dto"
    "model\dto\ReferenceDto.java"                            = "$basePackage.model.dto"
    "model\dto\RunProgramRequest.java"                       = "$basePackage.model.dto"
    "model\dto\SiliconFlowEmbeddingRequest.java"             = "$basePackage.model.dto"
    "model\dto\SiliconFlowEmbeddingResponse.java"            = "$basePackage.model.dto"
    "model\dto\SiliconFlowRerankRequest.java"                = "$basePackage.model.dto"
    "model\dto\SiliconFlowRerankResponse.java"               = "$basePackage.model.dto"
    "model\dto\UpdateChatSessionRequest.java"                = "$basePackage.model.dto"
    "model\dto\WeatherRequest.java"                          = "$basePackage.model.dto"
    "model\dto\WeatherResponse.java"                         = "$basePackage.model.dto"
    "exception\GlobalExceptionHandler.java"                  = "$basePackage.exception"
    "exception\GenericChatApiException.java"                 = "$basePackage.exception"
    "exception\SiliconFlowApiException.java"                 = "$basePackage.exception"
    "exception\SiliconFlowRerankException.java"              = "$basePackage.exception"
    "exception\ToolApprovalRequiredException.java"           = "$basePackage.exception"
    "core\advisor\HumanInTheLoopAdvisor.java"                = "$basePackage.core.advisor"
    "core\client\GenericChatClient.java"                     = "$basePackage.core.client"
    "core\client\RerankClient.java"                          = "$basePackage.core.client"
    "core\client\SiliconFlowEmbeddingClient.java"            = "$basePackage.core.client"
    "core\model\DeterministicEmbeddingModel.java"            = "$basePackage.core.model"
    "core\tool\DateTool.java"                                = "$basePackage.core.tool"
    "core\tool\RunProgramTool.java"                          = "$basePackage.core.tool"
    "core\tool\WeatherTool.java"                             = "$basePackage.core.tool"
    "core\formatter\DefaultResponseFormatter.java"           = "$basePackage.core.formatter"
    "core\formatter\ResponseFormatter.java"                  = "$basePackage.core.formatter"
    "core\preprocessor\QueryPreprocessor.java"               = "$basePackage.core.preprocessor"
    "core\prompt\DynamicPromptBuilder.java"                  = "$basePackage.core.prompt"
}

# ======================================================
# Step 2: 先收集所有文件当前的 package 声明（用于构建替换映射）
# ======================================================
$oldToNew = @{}  # oldPackage -> newPackage

foreach ($relativePath in $packageMap.Keys) {
    $fullPath = Join-Path $baseDir $relativePath
    if (-not (Test-Path $fullPath)) {
        Write-Warning "文件不存在，跳过: $fullPath"
        continue
    }
    $content = Get-Content $fullPath -Raw -Encoding UTF8
    if ($content -match 'package\s+([\w.]+);') {
        $oldPkg = $matches[1]
        $newPkg = $packageMap[$relativePath]
        if ($oldPkg -ne $newPkg) {
            $oldToNew[$oldPkg] = $newPkg
            Write-Host "  待修复: $relativePath"
            Write-Host "    旧: $oldPkg  →  新: $newPkg"
        }
    }
}

Write-Host ""
Write-Host "=== 共有 $($oldToNew.Count) 个唯一 package 需要替换 ==="
Write-Host ""

# ======================================================
# Step 3: 对每个目标文件修复 package 声明
# ======================================================
Write-Host "--- Phase 1: 修复 package 声明 ---"
foreach ($relativePath in $packageMap.Keys) {
    $fullPath = Join-Path $baseDir $relativePath
    if (-not (Test-Path $fullPath)) { continue }

    $content = Get-Content $fullPath -Raw -Encoding UTF8
    $newPkg = $packageMap[$relativePath]
    $newContent = $content -replace 'package\s+[\w.]+;', "package $newPkg;"

    if ($newContent -ne $content) {
        [System.IO.File]::WriteAllText($fullPath, $newContent, [System.Text.UTF8Encoding]::new($false))
        Write-Host "  [FIXED package] $relativePath"
    }
}

# ======================================================
# Step 4: 对所有 Java 文件做全局 import 替换
# ======================================================
Write-Host ""
Write-Host "--- Phase 2: 全局修复 import ---"

$allJavaFiles = Get-ChildItem -Path $baseDir -Recurse -Filter "*.java"
foreach ($file in $allJavaFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $newContent = $content

    foreach ($oldPkg in $oldToNew.Keys) {
        $newPkg = $oldToNew[$oldPkg]
        # 替换 import 语句（精确匹配，避免误替换包含相同前缀的其他包名）
        $newContent = $newContent -replace "import\s+$([Regex]::Escape($oldPkg))\.([\w*]+);", "import $newPkg.`$1;"
    }

    if ($newContent -ne $content) {
        [System.IO.File]::WriteAllText($file.FullName, $newContent, [System.Text.UTF8Encoding]::new($false))
        Write-Host "  [FIXED import] $($file.Name)"
    }
}

# ======================================================
# Step 5: 将 GenerationService、RetrievalService 的物理文件移动到 core/rag/
# ======================================================
Write-Host ""
Write-Host "--- Phase 3: 移动 RetrievalService、GenerationService 到 core/rag/ ---"

$ragDir = Join-Path $baseDir "core\rag"
if (-not (Test-Path $ragDir)) {
    New-Item -ItemType Directory -Path $ragDir -Force | Out-Null
    Write-Host "  创建目录: core\rag\"
}

$moveTargets = @("service\GenerationService.java", "service\RetrievalService.java")
foreach ($rel in $moveTargets) {
    $src = Join-Path $baseDir $rel
    if (Test-Path $src) {
        Move-Item -Path $src -Destination $ragDir -Force
        Write-Host "  [MOVED] $rel → core\rag\"
    }
}

Write-Host ""
Write-Host "=== 所有操作完成 ==="
