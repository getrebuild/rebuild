# REBUILD one-click build
# Usage:
#   .\build.ps1
#   .\build.ps1 -Mob
#   .\build.ps1 -Mob -MobDir D:\path\rebuild-mob

param(
    [switch]$Mob,
    [string]$MobDir = "$PSScriptRoot\..\rebuild-mob"
)

$ErrorActionPreference = "Stop"

$NodeVersion  = "v20.18.0"
# $NodeDistUrl = "https://mirrors.tuna.tsinghua.edu.cn/nodejs-release"
$NodeDistUrl  = "https://nodejs.org/dist"
$NodeDir      = "$PSScriptRoot\.deploy\node"

function Ensure-Node {
    # 1) system node on PATH
    if (Get-Command node -ErrorAction SilentlyContinue) {
        Write-Host "Using system Node: $(& node --version)" -ForegroundColor DarkGray
        return
    }
    # 2) previously downloaded portable node
    if (Test-Path "$NodeDir\node.exe") {
        $env:Path = "$NodeDir;$env:Path"
        Write-Host "Using portable Node: $(& node --version)" -ForegroundColor DarkGray
        return
    }
    # 3) download portable node
    $arch = if ([Environment]::Is64BitOperatingSystem) { "win-x64" } else { "win-x86" }
    $zip  = "node-$NodeVersion-$arch.zip"
    $url  = "$NodeDistUrl/$NodeVersion/$zip"
    $tmpZip = "$env:TEMP\$zip"
    $tmp    = "$NodeDir.tmp"
    Write-Host "Node.js not found. Downloading portable $NodeVersion ($arch) ..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $url -OutFile $tmpZip -UseBasicParsing
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path $tmpZip -DestinationPath $tmp -Force
    Remove-Item $tmpZip -Force
    New-Item -ItemType Directory -Force -Path $NodeDir | Out-Null
    $top = Get-ChildItem $tmp -Directory | Select-Object -First 1
    Get-ChildItem $top.FullName | Move-Item -Destination $NodeDir -Force
    Remove-Item $tmp -Recurse -Force
    $env:Path = "$NodeDir;$env:Path"
    Write-Host "Installed portable Node: $(& node --version)" -ForegroundColor Green
}

Ensure-Node

if ($Mob) {
    if (Test-Path "$MobDir\package.json") {
        Push-Location $MobDir
        yarn install
        yarn build
        robocopy build "$PSScriptRoot\src\main\resources\public\h5app" /MIR /NJH /NJS /NFL /NDL
        Pop-Location
    }
    else {
        Write-Warning "rebuild-mob not found at '$MobDir', skip building h5app. Use -MobDir to specify the path."
    }
}

& "$PSScriptRoot\mvnw.cmd" clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

Write-Host "Done: $PSScriptRoot\target\rebuild.jar" -ForegroundColor Green
