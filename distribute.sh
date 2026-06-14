#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Terminal colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}          Associations Desktop Packaging Tool        ${NC}"
echo -e "${BLUE}====================================================${NC}"

# Detect Operating System
OS="$(uname -s)"
case "${OS}" in
    Linux*)     MACHINE=Linux;;
    Darwin*)    MACHINE=Mac;;
    CYGWIN*)    MACHINE=Windows;;
    MINGW*)     MACHINE=Windows;;
    *)          MACHINE="UNKNOWN:${OS}"
esac

echo -e "Detected OS: ${GREEN}${MACHINE}${NC}"

if [ "$MACHINE" = "Mac" ]; then
    echo -e "Building macOS package (.dmg)..."
    ./gradlew :composeApp:packageReleaseDmg
    
    DMG_PATH="composeApp/build/compose/binaries/main/dmg"
    echo -e "${GREEN}✓ Success!${NC} macOS DMG created."
    echo -e "Location: ${YELLOW}$(pwd)/${DMG_PATH}${NC}"
    ls -l "${DMG_PATH}" 2>/dev/null || true

elif [ "$MACHINE" = "Windows" ]; then
    echo -e "Building Windows package (.msi)..."
    ./gradlew :composeApp:packageReleaseMsi
    
    MSI_PATH="composeApp/build/compose/binaries/main/msi"
    echo -e "${GREEN}✓ Success!${NC} Windows MSI installer created."
    echo -e "Location: ${YELLOW}$(pwd)/${MSI_PATH}${NC}"
    ls -l "${MSI_PATH}" 2>/dev/null || true

else
    echo -e "${YELLOW}Warning: Cross-compilation for native packages (.dmg, .msi) is not supported by Compose Multiplatform.${NC}"
    echo -e "To package for ${RED}macOS M1${NC}, you must run this script on a Mac."
    echo -e "To package for ${RED}Windows 10/11${NC}, you must run this script (or gradlew.bat) on a Windows machine."
    echo -e ""
    echo -e "Alternatively, you can build a cross-platform runnable JAR (requires Java installed to run):"
    echo -e "Running: ./gradlew :composeApp:packageReleaseUberJarForCurrentOS"
    ./gradlew :composeApp:packageReleaseUberJarForCurrentOS
    
    JAR_PATH="composeApp/build/compose/binaries/main/jar"
    echo -e "${GREEN}✓ Success!${NC} Cross-platform JAR created."
    echo -e "Location: ${YELLOW}$(pwd)/${JAR_PATH}${NC}"
fi

echo -e "${BLUE}====================================================${NC}"
