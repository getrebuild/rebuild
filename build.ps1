# REBUILD one-click build
# Usage:
#   .\build.ps1                 -> package rebuild only
#   .\build.ps1 -Mob            -> build rebuild-mob first, then rebuild
#   .\build.ps1 -Mob -MobDir D:\path\rebuild-mob

param(
    [switch]$Mob,
    [string]$MobDir = "$PSScriptRoot\..\rebuild-mob"
)

$ErrorActionPreference = "Stop"

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
