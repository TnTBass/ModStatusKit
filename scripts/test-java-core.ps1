$ErrorActionPreference = "Stop"

$buildDir = "build\test-classes"
if (Test-Path $buildDir) {
    Remove-Item -Recurse -Force $buildDir
}
New-Item -ItemType Directory -Path $buildDir | Out-Null

$mainSources = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$testSources = Get-ChildItem -Path "src\test\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

& javac -d $buildDir $mainSources $testSources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& java -cp $buildDir dev.jasmine.modstatuskit.ModStatusKitTest
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
