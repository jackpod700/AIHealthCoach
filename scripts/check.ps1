$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock] $Command
    )

    & $Command

    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE"
    }
}

function Invoke-HarnessScript {
    param(
        [Parameter(Mandatory = $true)]
        [string] $ScriptPath,

        [Parameter(Mandatory = $true)]
        [string] $Label,

        [Parameter(Mandatory = $true)]
        [string] $WorkingDir
    )

    if (-not (Test-Path -LiteralPath $ScriptPath -PathType Leaf)) {
        return
    }

    Write-Host ""
    Write-Host "[$Label] running $ScriptPath"

    Push-Location $WorkingDir
    try {
        $extension = [System.IO.Path]::GetExtension($ScriptPath)
        $workingFullPath = (Resolve-Path -LiteralPath $WorkingDir).Path.TrimEnd("\", "/")
        $scriptFullPath = (Resolve-Path -LiteralPath $ScriptPath).Path

        if ($scriptFullPath.StartsWith($workingFullPath)) {
            $relativeScriptPath = $scriptFullPath.Substring($workingFullPath.Length).TrimStart("\", "/")
        }
        else {
            $relativeScriptPath = $scriptFullPath
        }

        $portableScriptPath = "./" + ($relativeScriptPath -replace "\\", "/")

        if ($extension -eq ".ps1") {
            Invoke-NativeCommand {
                powershell -NoProfile -ExecutionPolicy Bypass -File $ScriptPath
            }
            return
        }

        $bash = Get-Command bash -ErrorAction SilentlyContinue
        if ($bash) {
            Invoke-NativeCommand {
                bash $portableScriptPath
            }
            return
        }

        $sh = Get-Command sh -ErrorAction SilentlyContinue
        if ($sh) {
            Invoke-NativeCommand {
                sh $portableScriptPath
            }
            return
        }

        Invoke-NativeCommand {
            & $ScriptPath
        }
    }
    finally {
        Pop-Location
    }
}

function Invoke-HarnessScripts {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Label,

        [Parameter(Mandatory = $true)]
        [string] $ProjectDir
    )

    $ScriptsDir = Join-Path $ProjectDir "harness/scripts"

    if (-not (Test-Path -LiteralPath $ScriptsDir -PathType Container)) {
        Write-Host ""
        Write-Host "[$Label] skip: $ScriptsDir not found"
        return
    }

    $scripts = Get-ChildItem -LiteralPath $ScriptsDir -File | Sort-Object Name

    if ($scripts.Count -eq 0) {
        Write-Host ""
        Write-Host "[$Label] skip: no scripts found in $ScriptsDir"
        return
    }

    foreach ($script in $scripts) {
        Invoke-HarnessScript `
            -ScriptPath $script.FullName `
            -Label $Label `
            -WorkingDir $ProjectDir
    }
}

Invoke-HarnessScripts -Label "frontend" -ProjectDir (Join-Path $RootDir "frontend")
Invoke-HarnessScripts -Label "backend" -ProjectDir (Join-Path $RootDir "backend")

Write-Host ""
Write-Host "All checks passed."
