#!/bin/bash
# download-deps.sh
# Runs during Codespaces PREBUILD (onCreateCommand).
# Downloads all dependencies so new codespaces start in seconds, not minutes.
# This script takes ~5 minutes but only runs once during prebuild.

set -e
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Installing Node 18 via nvm..."
export NVM_DIR="/usr/local/share/nvm"
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
nvm install 18 --default 2>/dev/null || true

echo "==> Downloading Gradle wrapper..."
chmod +x gradlew
./gradlew --version --no-daemon -q

echo "==> Downloading all Gradle dependencies..."
./gradlew dependencies --no-daemon -q 2>&1 | tail -5

echo "==> Installing npm dependencies..."
cd web && npm install --silent && cd ..

echo "Prebuild complete."
