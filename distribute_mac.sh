#!/bin/bash

# Terminal colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

LOG_FILE="build_mac_dmg.log"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}       Associations - macOS DMG Packaging            ${NC}"
echo -e "${BLUE}====================================================${NC}"
echo -e "Log file: ${YELLOW}${LOG_FILE}${NC}"
echo -e ""

echo "Build started at: $(date)" > "$LOG_FILE"

echo -e "Building macOS DMG installer..."
echo -e "(This may take a few minutes...)"
echo -e ""

if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
    echo -e "Detected Homebrew OpenJDK 17. Using JAVA_HOME: ${YELLOW}${JAVA_HOME}${NC}"
    echo -e ""
fi

rm -rf composeApp/build/compose/binaries/main/dmg

chmod +x ./gradlew

if ./gradlew :composeApp:packageDmg 2>&1 | tee -a "$LOG_FILE"; then
    DMG_PATH="composeApp/build/compose/binaries/main/dmg"
    echo -e ""
    echo -e "${GREEN}✓ SUCCESS!${NC} DMG installer created."
    echo -e "Location: ${YELLOW}${DMG_PATH}${NC}"
    echo -e ""
    ls -lh "${DMG_PATH}" 2>/dev/null || echo "Folder not found"
    
    if [ "$(uname -s)" = "Darwin" ]; then
        open "${DMG_PATH}" 2>/dev/null
    fi
else
    echo -e ""
    echo -e "${RED}✗ BUILD FAILED!${NC}"
    echo -e "See the full error above or open the log file: ${YELLOW}${LOG_FILE}${NC}"
fi

echo -e ""
echo -e "${BLUE}====================================================${NC}"
echo -e "Press ENTER to close this window..."
read -r
