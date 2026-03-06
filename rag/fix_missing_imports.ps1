$ErrorActionPreference = "Stop"
$srcDir = "d:\project\assistant-demo\rag\src"
$bp = "com.example.springaialibaba"

# 针对同包引用的文件，直接指定需要添加的 import
# key = 文件相对于 src\main\java 的路径, value = 要插入的 import 行列表
$addImports = @{
    "com\example\springaialibaba\core\client\GenericChatClient.java" = @(
        "import $bp.config.properties.GenericChatProperties;"
    )
    "com\example\springaialibaba\core\client\RerankClient.java" = @(
        "import $bp.config.properties.SiliconFlowRerankProperties;"
    )
    "com\example\springaialibaba\core\client\SiliconFlowEmbeddingClient.java" = @(
        "import $bp.config.properties.SiliconFlowEmbeddingProperties;"
    )
    "com\example\springaialibaba\core\prompt\DynamicPromptBuilder.java" = @(
        "import $bp.config.properties.PromptProperties;"
    )
    "com\example\springaialibaba\core\tool\RunProgramTool.java" = @(
        "import $bp.model.dto.RunProgramRequest;"
    )
    "com\example\springaialibaba\service\AgentService.java" = @(
        "import $bp.model.entity.PendingApproval;",
        "import $bp.exception.ToolApprovalRequiredException;"
    )
    "com\example\springaialibaba\service\AdvisorHitlService.java" = @(
        "import $bp.model.entity.PendingApproval;",
        "import $bp.exception.ToolApprovalRequiredException;"
    )
    "com\example\springaialibaba\service\ApprovalService.java" = @(
        "import $bp.model.entity.PendingApproval;",
        "import $bp.exception.ToolApprovalRequiredException;"
    )
}

$javaRoot = "$srcDir\main\java"

foreach ($relPath in $addImports.Keys) {
    $fullPath = Join-Path $javaRoot $relPath
    if (-not (Test-Path $fullPath)) {
        Write-Warning "文件不存在: $fullPath"
        continue
    }
    $content = Get-Content $fullPath -Raw -Encoding UTF8
    $importsToAdd = $addImports[$relPath]
    $changed = $false

    foreach ($imp in $importsToAdd) {
        if ($content -notmatch [Regex]::Escape($imp)) {
            # 在第一个 import 语句前插入，或在 package 语句后插入
            if ($content -match "^(package[^\r\n]+[\r\n]+[\r\n]*)") {
                $insertAfter = $matches[1]
                $content = $content.Replace($insertAfter, $insertAfter + $imp + "`r`n")
                $changed = $true
                Write-Host "  [ADD] $(Split-Path $relPath -Leaf): $imp"
            }
        }
    }

    if ($changed) {
        [System.IO.File]::WriteAllText($fullPath, $content, [System.Text.UTF8Encoding]::new($false))
    }
}

Write-Host ""
Write-Host "=== 完成 ==="
