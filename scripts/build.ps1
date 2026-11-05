<#
.SYNOPSIS
  Runs a Gradle task against this project. Now that the project lives on a
  native NTFS path (moved off the \\wsl.localhost UNC mount), this is just a
  thin convenience wrapper around gradlew.bat — no UNC cwd or Gradle
  cache-locking workarounds are needed anymore.

.EXAMPLE
  scripts\build.ps1 assembleDebug
  scripts\build.ps1 installDebug -Stacktrace
#>
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Tasks,
    [switch]$Stacktrace
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
}

$taskArgs = @($Tasks)
if ($Stacktrace) { $taskArgs += "--stacktrace" }

Push-Location $repoRoot
try {
    & .\gradlew.bat @taskArgs
} finally {
    Pop-Location
}
