<#
.SYNOPSIS
    Generates an original Russian speech WAV using the Windows SAPI voice,
    then wraps it into an MP4 with FFmpeg. Content is synthetic/original.
.EXAMPLE
    ./scripts/generate-speech-fixture.ps1
#>
param(
    [string]$TextFile = "$PSScriptRoot/../demos/fixtures/speech-ru.txt",
    [string]$Wav      = "$PSScriptRoot/../demos/fixtures/speech-ru.wav",
    [string]$Mp4      = "$PSScriptRoot/../demos/fixtures/sample-lecture-ru.mp4",
    [string]$Voice    = "Microsoft Irina Desktop",
    [string]$Ffmpeg   = "ffmpeg"
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech

$text = [System.IO.File]::ReadAllText((Resolve-Path $TextFile), [System.Text.Encoding]::UTF8)
New-Item -ItemType Directory -Force -Path (Split-Path $Wav) | Out-Null

$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
try { $synth.SelectVoice($Voice) } catch { Write-Warning "Voice '$Voice' unavailable, using default." }
$synth.Rate = 0
New-Item -ItemType Directory -Force -Path (Split-Path $Wav) | Out-Null
$synth.SetOutputToWaveFile($Wav)
$synth.Speak($text)
$synth.Dispose()
Write-Host "WAV written: $Wav"

& $Ffmpeg -y -f lavfi -i "color=c=0x101030:s=640x360:r=10" -i $Wav `
    -c:v libx264 -pix_fmt yuv420p -c:a aac -shortest $Mp4
Write-Host "MP4 written: $Mp4"
