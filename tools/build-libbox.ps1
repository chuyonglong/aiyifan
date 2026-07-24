param(
    [string]$AndroidSdkRoot = $env:ANDROID_HOME,
    [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = "Stop"
$expectedRevision = "4626aa2cb07db5a453f689cf348ba9d327e07820"
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot "third_party\sing-box"
$outputPath = Join-Path $projectRoot "app\libs\libbox.aar"

if ([string]::IsNullOrWhiteSpace($AndroidSdkRoot)) {
    $localProperties = Join-Path $projectRoot "local.properties"
    $sdkLine = Get-Content $localProperties -ErrorAction SilentlyContinue |
        Where-Object { $_ -match "^sdk\.dir=" } |
        Select-Object -First 1
    if ($sdkLine) {
        $AndroidSdkRoot = ($sdkLine -replace "^sdk\.dir=", "").Replace('\:', ':').Replace('\\', '\')
    }
}

function Test-OpenJdk17([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path)) { return $false }
    $java = Join-Path $Path "bin\java.exe"
    if (-not (Test-Path $java)) { return $false }
    return ((& $java --version 2>&1 | Out-String) -match "openjdk 17")
}

if (-not (Test-Path $sourceRoot)) {
    throw "Missing third_party/sing-box. Run git submodule update --init --recursive first."
}
if ([string]::IsNullOrWhiteSpace($AndroidSdkRoot) -or -not (Test-Path $AndroidSdkRoot)) {
    throw "Set ANDROID_HOME or pass -AndroidSdkRoot to a valid Android SDK."
}
if (-not (Test-OpenJdk17 $JavaHome)) {
    $JavaHome = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-OpenJdk17 $_.FullName } |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not (Test-OpenJdk17 $JavaHome)) {
    throw "Set JAVA_HOME or pass -JavaHome to an OpenJDK 17 installation."
}

$revision = (git -C $sourceRoot rev-parse HEAD).Trim()
if ($revision -ne $expectedRevision) {
    throw "sing-box must be pinned to $expectedRevision, found $revision."
}

$env:ANDROID_HOME = $AndroidSdkRoot
$env:ANDROID_SDK_ROOT = $AndroidSdkRoot
$env:JAVA_HOME = $JavaHome
$goBin = Join-Path (go env GOPATH) "bin"
$env:PATH = "$goBin;$env:PATH"
go install github.com/sagernet/gomobile/cmd/gomobile@v0.1.8
Push-Location $sourceRoot
try {
    Remove-Item -LiteralPath "build" -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath "libbox.aar" -Force -ErrorAction SilentlyContinue
    & (Join-Path $goBin "gomobile.exe") init
    go run ./cmd/internal/build_libbox -target android
    New-Item -ItemType Directory -Force (Split-Path -Parent $outputPath) | Out-Null
    Copy-Item -LiteralPath "libbox.aar" -Destination $outputPath -Force
} finally {
    Pop-Location
}

Get-FileHash $outputPath -Algorithm SHA256
