<#
.SYNOPSIS
    Generates a short synthetic lecture video fixture with FFmpeg.
.EXAMPLE
    ./demos/fixtures/generate-fixture.ps1 -Output demos/fixtures/sample-lecture.mp4
#>
param(
    [string]$Output = "demos/fixtures/sample-lecture.mp4",
    [int]$Seconds = 30,
    [string]$Ffmpeg = "ffmpeg"
)
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path (Split-Path $Output) | Out-Null
& $Ffmpeg -y `
    -f lavfi -i "testsrc2=size=640x360:rate=10" `
    -f lavfi -i "sine=frequency=440" `
    -t $Seconds -c:v libx264 -pix_fmt yuv420p -c:a aac -shortest $Output
Write-Host "Fixture written to $Output"
