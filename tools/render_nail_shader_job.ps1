param(
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$Open
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $JavaHome) {
    $JavaHome = Join-Path $env:USERPROFILE '.gradle/jdks/jetbrains_s_r_o_-21-amd64-windows.2'
}
Push-Location $projectRoot
try {
    & "$JavaHome/bin/javac.exe" -d build/preview-model common/src/main/java/com/nstut/celestialnail/client/CelestialNailMesh.java tools/ExportNailMesh.java
    if ($LASTEXITCODE) { throw 'Model compilation failed' }
    & "$JavaHome/bin/java.exe" -cp build/preview-model ExportNailMesh | Out-File -Encoding ascii build/nail-mesh.txt
    if ($LASTEXITCODE) { throw 'Mesh export failed' }
    python tools/preview_crystal_shader.py
    if ($LASTEXITCODE) { throw 'Shader preview failed' }
    if ($Open) { Invoke-Item (Join-Path $projectRoot 'build/crystal-glint-preview/preview.html') }
} finally { Pop-Location }
