$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$sources = Get-ChildItem -Path "$root\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$outDir = "$root\out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Write-Host "Compiling..."
& javac -d $outDir $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Starting server on http://localhost:8080 ..."
& java -cp $outDir com.library.Main
