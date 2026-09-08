<#
.SYNOPSIS
  Runs a Gradle task against this project from Windows, working around two
  quirks of building on a \\wsl.localhost UNC path:

  1. cmd.exe (which .bat wrapper scripts always shell out through) cannot
     start with a UNC path as its working directory, and its bare-name
     command lookup doesn't resolve on the WSL 9P-mapped drive either -
     so gradlew.bat must be invoked by its fully-qualified mapped-drive path.
  2. Gradle's per-project cache needs real Win32 file locking, which the
     WSL 9P network provider does not support (`IOException: Incorrect
     function`) - so --project-cache-dir is redirected to a local folder.
     Project files themselves stay exactly where they are; only Gradle's
     internal lock-needing cache moves.

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
$repoUnc = "\\wsl.localhost\Ubuntu\var\www\html\Kumbuka"
$cacheDir = "C:\Users\Alvin\.gradle-caches\Kumbuka"
New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
}

$taskArgs = ($Tasks -join " ")
$extra = if ($Stacktrace) { "--stacktrace" } else { "" }

Set-Location C:\
cmd.exe /c "pushd $repoUnc && Z:\var\www\html\Kumbuka\gradlew.bat $taskArgs --project-cache-dir $cacheDir $extra"
Set-Location $repoUnc
