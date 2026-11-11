# Stages all 4 verified AZYRON CLIENT v1.0.2 release assets on GitHub
# while keeping the release strictly in DRAFT mode (unpublished).
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ClientRoot = Resolve-Path (Join-Path $ScriptDir "..")
$ReleasesDir = Join-Path $ClientRoot "releases"
$NotesFile = Join-Path $ClientRoot "RELEASE_NOTES_v1.0.2.md"
$Repo = if ($env:GITHUB_REPOSITORY) { $env:GITHUB_REPOSITORY } else { "AzyronClientteam/Azyron-Client" }
$Tag = "v1.0.2"

$JarFile = Join-Path $ReleasesDir "Azyron-Client-1.0.2.jar"
$MrpackFile = Join-Path $ReleasesDir "Azyron-Modpack-1.0.2.mrpack"
$SrcZip = Join-Path $ReleasesDir "Azyron-Client-Source-v1.0.2.zip"
$SrcTgz = Join-Path $ReleasesDir "Azyron-Client-Source-v1.0.2.tar.gz"

foreach ($f in @($JarFile, $MrpackFile, $SrcZip, $SrcTgz, $NotesFile)) {
    if (!(Test-Path $f) -or ((Get-Item $f).Length -eq 0)) {
        throw "Missing or empty required release file: $f"
    }
}

gh release view $Tag -R $Repo *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Uploading assets to existing Draft Release $Tag on $Repo..."
    gh release upload $Tag `
        "$JarFile#Azyron-Client-1.0.2.jar" `
        "$MrpackFile#Azyron-Modpack-1.0.2.mrpack" `
        "$SrcZip#Azyron-Client-Source-v1.0.2.zip" `
        "$SrcTgz#Azyron-Client-Source-v1.0.2.tar.gz" `
        --clobber `
        -R $Repo
} else {
    Write-Host "Creating UNPUBLISHED Draft Release $Tag on $Repo (--draft)..."
    gh release create $Tag `
        "$JarFile#Azyron-Client-1.0.2.jar" `
        "$MrpackFile#Azyron-Modpack-1.0.2.mrpack" `
        "$SrcZip#Azyron-Client-Source-v1.0.2.zip" `
        "$SrcTgz#Azyron-Client-Source-v1.0.2.tar.gz" `
        --draft `
        --title "AZYRON CLIENT v1.0.2 (Minecraft 1.21.11 Fabric)" `
        --notes-file $NotesFile `
        -R $Repo
}

gh release view $Tag -R $Repo --json tagName,name,isDraft,isPrerelease,assets
