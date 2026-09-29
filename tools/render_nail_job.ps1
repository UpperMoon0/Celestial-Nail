param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$Output = "docs/celestial-nail-model-preview.png"
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
    python tools/preview_nail.py --output $Output
    if ($LASTEXITCODE) { throw 'Preview render failed' }
} finally { Pop-Location }
