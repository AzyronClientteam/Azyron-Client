# Uploads or stages all 4 verified AZYRON CLIENT v0.1.0-pre release assets on GitHub
# while keeping the release strictly in DRAFT PRE-RELEASE mode (unpublished).
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ClientRoot = Resolve-Path (Join-Path $ScriptDir "..")
$ReleasesDir = Join-Path $ClientRoot "releases"
$NotesFile = Join-Path $ClientRoot "RELEASE_NOTES_v0.1.0-pre.md"
$Repo = if ($env:GITHUB_REPOSITORY) { $env:GITHUB_REPOSITORY } else { "AzyronClientteam/Azyron-Client" }
$Tag = "v0.1.0-pre"

$JarFile = Join-Path $ReleasesDir "Azyron-Client-1.0.1.jar"
$MrpackFile = Join-Path $ReleasesDir "Azyron-Modpack-1.0.mrpack"
$SrcZip = Join-Path $ReleasesDir "Azyron-Client-Source-v1.0.1.zip"
$SrcTgz = Join-Path $ReleasesDir "Azyron-Client-Source-v1.0.1.tar.gz"

foreach ($f in @($JarFile, $MrpackFile, $SrcZip, $SrcTgz, $NotesFile)) {
    if (!(Test-Path $f) -or ((Get-Item $f).Length -eq 0)) {
        throw "Missing or empty required release file: $f"
    }
}

gh release view $Tag -R $Repo *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Uploading source archives to existing Draft Pre-Release $Tag on $Repo..."
    gh release upload $Tag `
        "$SrcZip#Azyron-Client-Source-v1.0.1.zip" `
        "$SrcTgz#Azyron-Client-Source-v1.0.1.tar.gz" `
        --clobber `
        -R $Repo
} else {
    Write-Host "Creating UNPUBLISHED Draft Pre-Release $Tag on $Repo (--draft --prerelease)..."
    gh release create $Tag `
        "$JarFile#Azyron-Client-1.0.1.jar" `
        "$MrpackFile#Azyron-Modpack-1.0.mrpack" `
        "$SrcZip#Azyron-Client-Source-v1.0.1.zip" `
        "$SrcTgz#Azyron-Client-Source-v1.0.1.tar.gz" `
        --draft `
        --prerelease `
        --title "AZYRON CLIENT v0.1.0 PRE-RELEASE" `
        --notes-file $NotesFile `
        -R $Repo
}

gh release view $Tag -R $Repo --json tagName,name,isDraft,isPrerelease,assets
