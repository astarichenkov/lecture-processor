#requires -Version 5.1
<#
.SYNOPSIS
    Restores a specific day's milestone into snapshots/<day> using git worktree.

.DESCRIPTION
    Creates a detached worktree from the annotated tag week6-<day>.
    Snapshots are ignored by git and never committed. Existing snapshot
    directories are never overwritten without an explicit -Force.

.EXAMPLE
    ./scripts/create-snapshot.ps1 -Day day27
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('day27', 'day28', 'day29', 'day30')]
    [string]$Day,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$tag = "week6-$Day"
$target = Join-Path $repoRoot "snapshots/$Day"

Push-Location $repoRoot
try {
    if (-not (git rev-parse -q --verify "refs/tags/$tag")) {
        throw "Tag $tag not found. Fetch tags first (git fetch --tags)."
    }
    if (Test-Path $target) {
        if (-not $Force) {
            throw "Snapshot path already exists: $target. Use -Force to recreate it safely."
        }
        Write-Host "Removing existing worktree $target"
        git worktree remove --force $target 2>$null
        if (Test-Path $target) { Remove-Item -Recurse -Force $target }
    }
    New-Item -ItemType Directory -Force -Path (Join-Path $repoRoot 'snapshots') | Out-Null
    Write-Host "Creating worktree for $tag at $target"
    git worktree add --detach $target $tag
    Write-Host "Snapshot ready: $target"
}
finally {
    Pop-Location
}
