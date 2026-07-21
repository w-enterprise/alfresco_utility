param(
    [string]$AppVersion = "1.0",
    [switch]$RunTests,
    [switch]$SignExecutable,
    [string]$CertificatePath,
    [string]$CertificatePassword,
    [string]$IconPath
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$artifactName = "Alfresco-Proprieta-Cartelle"
$mainJarName = "$artifactName.jar"
$mainClass = "it.welf.alfresco.folderprops.App"

$buildInputDir = Join-Path $projectRoot "build\windows\input"
$distDir = Join-Path $projectRoot "dist"
$distWindowsDir = Join-Path $distDir "windows"
$distAppDir = Join-Path $distWindowsDir $artifactName
$zipPath = Join-Path $distDir "$artifactName.zip"

function Ensure-EmptyDir([string]$path) {
    if (Test-Path $path) {
        Remove-Item -Recurse -Force $path
    }
    New-Item -ItemType Directory -Force -Path $path | Out-Null
}

function Resolve-IconPath() {
    if ($IconPath -and (Test-Path $IconPath)) {
        return (Resolve-Path $IconPath).Path
    }
    $candidate1 = Join-Path $projectRoot "assets\$artifactName.ico"
    if (Test-Path $candidate1) {
        return (Resolve-Path $candidate1).Path
    }
    $candidate2 = Join-Path $projectRoot "target\windows-package\AlfrescoProprietaCartelle\AlfrescoProprietaCartelle.ico"
    if (Test-Path $candidate2) {
        return (Resolve-Path $candidate2).Path
    }
    return $null
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven (mvn) non trovato nel PATH."
}
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage non trovato nel PATH. Serve un JDK 14+ (consigliato JDK 17)."
}

Push-Location $projectRoot
try {
    if ($RunTests) {
        mvn -q test
    }

    mvn -q -DskipTests package

    Ensure-EmptyDir $buildInputDir
    $jarSource = Join-Path $projectRoot "target\$mainJarName"
    if (-not (Test-Path $jarSource)) {
        throw "Jar non trovato: $jarSource"
    }
    Copy-Item -Force $jarSource (Join-Path $buildInputDir $mainJarName)

    if (-not (Test-Path $distWindowsDir)) {
        New-Item -ItemType Directory -Force -Path $distWindowsDir | Out-Null
    }
    if (Test-Path $distAppDir) {
        Remove-Item -Recurse -Force $distAppDir
    }

    $iconResolved = Resolve-IconPath
    $jpackageArgs = @(
        "--type", "app-image",
        "--name", $artifactName,
        "--app-version", $AppVersion,
        "--input", $buildInputDir,
        "--main-jar", $mainJarName,
        "--main-class", $mainClass,
        "--dest", $distWindowsDir
    )

    if ($iconResolved) {
        $jpackageArgs += @("--icon", $iconResolved)
    }

    & jpackage @jpackageArgs

    $configSrc = Join-Path $projectRoot "alfresco_folderprops_config.properties"
    if (Test-Path $configSrc) {
        Copy-Item -Force $configSrc (Join-Path $distAppDir "alfresco_folderprops_config.properties")
    }

    if (Test-Path $zipPath) {
        Remove-Item -Force $zipPath
    }
    Compress-Archive -Path $distAppDir -DestinationPath $zipPath

    if ($SignExecutable) {
        if (-not $CertificatePath -or -not $CertificatePassword) {
            throw "Per firmare serve -CertificatePath e -CertificatePassword."
        }
        $exePath = Join-Path $distAppDir "$artifactName.exe"
        if (-not (Test-Path $exePath)) {
            throw "Exe non trovato per la firma: $exePath"
        }
        if (-not (Get-Command signtool.exe -ErrorAction SilentlyContinue)) {
            throw "signtool.exe non trovato. Installare Windows SDK oppure aggiungere signtool al PATH."
        }
        & signtool.exe sign /f $CertificatePath /p $CertificatePassword /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 $exePath
    }

    Write-Host "OK - dist generata:"
    Write-Host "  App: $distAppDir"
    Write-Host "  Zip: $zipPath"
} finally {
    Pop-Location
}
