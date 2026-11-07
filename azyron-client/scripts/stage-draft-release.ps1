# Stages an UNPUBLISHED Draft Release (v1.0) on GitHub (--draft only, never auto-published).
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ClientRoot = Resolve-Path (Join-Path $ScriptDir "..")
$ReleasesDir = Join-Path $ClientRoot "releases"

$JarFile = Join-Path $ReleasesDir "Azyron-Client-1.0.jar"
$MrpackFile = Join-Path $ReleasesDir "Azyron-Modpack-1.0.mrpack"

if (!(Test-Path $JarFile) -or !(Test-Path $MrpackFile)) {
    throw "Missing build artifacts in $ReleasesDir"
}

$Notes = @"
**AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.**
High-FPS Minecraft Java Edition **1.21.11** Fabric client developed by **JIGNESH GAMING**, engineered for desktop launchers and **PojavLauncher**.

### Highlights
- **Target Platform:** Minecraft Java Edition ``1.21.11`` (Fabric Loader ``>=0.17.3``, Java 21).
- **Performance Profiles:** *Potato Mode*, *Balanced Mode*, and *Smooth FPS Mode* with particle throttling, frame-spike stabilization, and safe memory trimming.
- **Client Menu & HUD Editor:** Open with ``Right Shift`` (or via the Pause Menu on PojavLauncher) to configure modules and drag/scale widgets.
- **Included Modules:** FPS Counter (1% Lows), CPS Counter, Ping Display, Coordinates HUD, Keystrokes HUD, Armor HUD, Potion Effects HUD, Memory HUD, Toggle Sprint, Smooth Zoom, and Custom Crosshair.
"@

$NotesFile = [System.IO.Path]::GetTempFileName()
Set-Content -Path $NotesFile -Value $Notes -Encoding UTF8

try {
    gh release create "v1.0" `
        "$JarFile#Azyron-Client-1.0.jar" `
        "$MrpackFile#Azyron-Modpack-1.0.mrpack" `
        --draft `
        --title "AZYRON CLIENT 1.0" `
        --notes-file $NotesFile
} finally {
    Remove-Item -Force $NotesFile -ErrorAction SilentlyContinue
}
