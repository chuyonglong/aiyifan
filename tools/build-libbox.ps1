param(
    [string]$AndroidSdkRoot = $env:ANDROID_HOME,
    [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = "Stop"
$expectedRevision = "4626aa2cb07db5a453f689cf348ba9d327e07820"
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot "third_party\sing-box"
$outputPath = Join-Path $projectRoot "app\libs\libbox.aar"

if (-not (Test-Path $sourceRoot)) {
    throw "Missing third_party/sing-box. Run git submodule update --init --recursive first."
}
if ([string]::IsNullOrWhiteSpace($AndroidSdkRoot) -or -not (Test-Path $AndroidSdkRoot)) {
    throw "Set ANDROID_HOME or pass -AndroidSdkRoot to a valid Android SDK."
}
if ([string]::IsNullOrWhiteSpace($JavaHome) -or -not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
    throw "Set JAVA_HOME or pass -JavaHome to an OpenJDK 17 installation."
}

$revision = (git -C $sourceRoot rev-parse HEAD).Trim()
if ($revision -ne $expectedRevision) {
    throw "sing-box must be pinned to $expectedRevision, found $revision."
}

$env:ANDROID_HOME = $AndroidSdkRoot
$env:ANDROID_SDK_ROOT = $AndroidSdkRoot
$env:JAVA_HOME = $JavaHome
$env:PATH = "$(go env GOPATH)\bin;$env:PATH"
Push-Location $sourceRoot
try {
    gomobile init
    go run ./cmd/internal/build_libbox -target android
    New-Item -ItemType Directory -Force (Split-Path -Parent $outputPath) | Out-Null
    Copy-Item -LiteralPath "libbox.aar" -Destination $outputPath -Force
} finally {
    Pop-Location
}

Get-FileHash $outputPath -Algorithm SHA256
