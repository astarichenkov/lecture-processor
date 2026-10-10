<#
.SYNOPSIS
    Reproducible installer for the Day 27 real toolchain (FFmpeg + whisper.cpp
    + Whisper model) on Windows. Ollama is installed separately (see below).

.DESCRIPTION
    Downloads and extracts:
      - FFmpeg "essentials" build (gyan.dev)
      - whisper.cpp prebuilt Windows BLAS binaries (ggml-org/whisper.cpp)
      - the selected ggml model from Hugging Face
    Nothing is written outside -ToolsRoot. If a component is already present it
    is skipped unless -Force is given.

    Ollama is intentionally NOT auto-installed because its installer is ~1.5 GB
    and requires the chosen model to be pulled afterwards:
      winget install Ollama.Ollama      # or: https://ollama.com/download
      ollama pull qwen3:4b

.EXAMPLE
    ./scripts/install-day27-tools.ps1
.EXAMPLE
    ./scripts/install-day27-tools.ps1 -ToolsRoot D:\ai -Model small -Force
#>
param(
    [string]$ToolsRoot = "C:\tools",
    [ValidateSet('tiny','base','small','medium','large-v3-turbo')]
    [string]$Model = "small",
    [string]$WhisperTag = "b5454",
    [switch]$Force
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Get-File($Url, $Dest) {
    if ((Test-Path $Dest) -and -not $Force) { Write-Host "  exists: $Dest"; return }
    Write-Host "  downloading $Url"
    Invoke-WebRequest -Uri $Url -OutFile $Dest
}

$downloads = Join-Path $ToolsRoot "downloads"
New-Item -ItemType Directory -Force -Path $downloads | Out-Null

# --- FFmpeg ---
$ffDir = Join-Path $ToolsRoot "ffmpeg"
if ($Force -or -not (Get-ChildItem -Path $ffDir -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue)) {
    $zip = Join-Path $downloads "ffmpeg.zip"
    Get-File "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip" $zip
    New-Item -ItemType Directory -Force -Path $ffDir | Out-Null
    Expand-Archive -Path $zip -DestinationPath $ffDir -Force
    Write-Host "FFmpeg installed -> $ffDir"
} else { Write-Host "FFmpeg already present -> $ffDir" }

# --- whisper.cpp ---
$whDir = Join-Path $ToolsRoot "whisper"
$binDir = Join-Path $whDir "bin"
$cli = Get-ChildItem -Path $binDir -Recurse -Filter whisper-cli.exe -ErrorAction SilentlyContinue | Select-Object -First 1
if ($Force -or -not $cli) {
    $zip = Join-Path $downloads "whisper-blas.zip"
    Get-File "https://github.com/ggml-org/whisper.cpp/releases/download/$WhisperTag/whisper-blas-bin-x64.zip" $zip
    New-Item -ItemType Directory -Force -Path $binDir | Out-Null
    Expand-Archive -Path $zip -DestinationPath $binDir -Force
    Write-Host "whisper.cpp installed -> $binDir"
} else { Write-Host "whisper.cpp already present -> $($cli.FullName)" }

# --- model ---
$modelsDir = Join-Path $whDir "models"
New-Item -ItemType Directory -Force -Path $modelsDir | Out-Null
$modelFile = Join-Path $modelsDir "ggml-$Model.bin"
Get-File "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-$Model.bin" $modelFile
Write-Host "model ready -> $modelFile"

# --- resolve paths ---
$ffmpeg = Get-ChildItem -Path $ffDir -Recurse -Filter ffmpeg.exe  | Select-Object -First 1
$ffprobe = Get-ChildItem -Path $ffDir -Recurse -Filter ffprobe.exe | Select-Object -First 1
$whisper = Get-ChildItem -Path $binDir -Recurse -Filter whisper-cli.exe | Select-Object -First 1

Write-Host ""
Write-Host "=== Add these environment variables (or pass as -D / application properties) ==="
Write-Host "FFMPEG_PATH=$($ffmpeg.FullName)"
Write-Host "FFPROBE_PATH=$($ffprobe.FullName)"
Write-Host "WHISPER_PATH=$($whisper.FullName)"
Write-Host "WHISPER_MODEL_PATH=$modelFile"
Write-Host ""
Write-Host "Next: install Ollama and pull the model:"
Write-Host "  winget install Ollama.Ollama"
Write-Host "  ollama pull qwen3:4b"
