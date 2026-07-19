$ErrorActionPreference = 'Stop'
$rootDir = (Get-Item $PSScriptRoot).Parent.FullName
$releaseDir = Join-Path $rootDir "suchika-release"

Write-Host "Building Suchika Release Distribution..." -ForegroundColor Cyan

# 1. Clean previous release
if (Test-Path $releaseDir) {
    Remove-Item -Path $releaseDir -Recurse -Force
}
New-Item -ItemType Directory -Path $releaseDir | Out-Null
New-Item -ItemType Directory -Path "$releaseDir/bin" | Out-Null
New-Item -ItemType Directory -Path "$releaseDir/bin/web-gateway" | Out-Null
New-Item -ItemType Directory -Path "$releaseDir/bin/profile" | Out-Null
New-Item -ItemType Directory -Path "$releaseDir/bin/wealth" | Out-Null
New-Item -ItemType Directory -Path "$releaseDir/bin/health" | Out-Null
New-Item -ItemType Directory -Path "$releaseDir/bin/household" | Out-Null

# 2. Build React Frontend
Write-Host "Building React Frontend..." -ForegroundColor Cyan
Set-Location -Path (Join-Path $rootDir "web")
npm install
npm run build

# 3. Copy React Build to Web-Gateway
Write-Host "Bundling Frontend into Web-Gateway..." -ForegroundColor Cyan
$gatewayResourcesDir = Join-Path $rootDir "application/web-gateway/src/main/resources/META-INF/resources"
if (!(Test-Path $gatewayResourcesDir)) {
    New-Item -ItemType Directory -Path $gatewayResourcesDir -Force | Out-Null
}
Copy-Item -Path "build\*" -Destination $gatewayResourcesDir -Recurse -Force

# 3.5 Copy Documents
Write-Host "Bundling Documents into Web-Gateway..." -ForegroundColor Cyan
$gatewayDocsDir = Join-Path $rootDir "application/web-gateway/src/main/resources/documents"
if (!(Test-Path $gatewayDocsDir)) {
    New-Item -ItemType Directory -Path $gatewayDocsDir -Force | Out-Null
}
Copy-Item -Path "documents\*" -Destination $gatewayDocsDir -Recurse -Force

# 4. Build Quarkus Backends
Write-Host "Compiling Java Backends (Fast-JAR)..." -ForegroundColor Cyan
Set-Location -Path $rootDir
./gradlew clean build -x test "-Dquarkus.package.type=fast-jar" --no-parallel
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed!"
    exit 1
}

# 5. Package into Release Directory
Write-Host "Packaging release artifacts..." -ForegroundColor Cyan
Copy-Item -Path "application/web-gateway/build/quarkus-app/*" -Destination "$releaseDir/bin/web-gateway" -Recurse -Force
Copy-Item -Path "application/domain/profile/adapters/build/quarkus-app/*" -Destination "$releaseDir/bin/profile" -Recurse -Force
Copy-Item -Path "application/domain/wealth/adapters/build/quarkus-app/*" -Destination "$releaseDir/bin/wealth" -Recurse -Force
Copy-Item -Path "application/domain/health/adapters/build/quarkus-app/*" -Destination "$releaseDir/bin/health" -Recurse -Force
Copy-Item -Path "application/domain/household/adapters/build/quarkus-app/*" -Destination "$releaseDir/bin/household" -Recurse -Force

# 6. Copy Scripts and Templates
Copy-Item -Path "scripts/release-template/start.bat" -Destination "$releaseDir/start.bat" -Force
Copy-Item -Path "scripts/release-template/.env.template" -Destination "$releaseDir/.env" -Force
Copy-Item -Path "scripts/release-template/setup-db.sql" -Destination "$releaseDir/setup-db.sql" -Force
Copy-Item -Path "scripts/release-template/README.txt" -Destination "$releaseDir/README.txt" -Force

# 7. Cleanup
Write-Host "Cleaning up temporary frontend assets from gateway..." -ForegroundColor Cyan
Remove-Item -Path $gatewayResourcesDir -Recurse -Force
Remove-Item -Path $gatewayDocsDir -Recurse -Force

Write-Host "Zipping the release..." -ForegroundColor Cyan
$zipPath = Join-Path $rootDir "suchika-release.zip"
if (Test-Path $zipPath) {
    Remove-Item -Path $zipPath -Force
}
Compress-Archive -Path $releaseDir -DestinationPath $zipPath

Write-Host "Release built successfully at $releaseDir" -ForegroundColor Green
Write-Host "ZIP file created at $zipPath" -ForegroundColor Green
