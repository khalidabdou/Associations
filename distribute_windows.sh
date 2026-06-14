#!/bin/bash

# Terminal colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

LOG_FILE="build_windows_msi.log"

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}       Associations - Windows MSI Packaging          ${NC}"
echo -e "${BLUE}====================================================${NC}"
echo -e "Log file: ${YELLOW}$(pwd)/${LOG_FILE}${NC}"
echo -e ""

echo "Build started at: $(date)" > "$LOG_FILE"

echo -e "Building Windows MSI installer..."
echo -e "(This may take a few minutes...)"
echo -e ""

# Run build and capture all output to log AND screen
if ./gradlew :composeApp:packageReleaseMsi 2>&1 | tee -a "$LOG_FILE"; then
    MSI_PATH="composeApp/build/compose/binaries/main/msi"
    echo -e ""
    echo -e "${GREEN}✓ SUCCESS!${NC} MSI installer created."
    echo -e "Location: ${YELLOW}$(pwd)/${MSI_PATH}${NC}"
    echo -e ""
    ls -l "${MSI_PATH}" 2>/dev/null || echo "Folder not found"
else
    echo -e ""
    echo -e "${RED}✗ BUILD FAILED!${NC}"
    echo -e "See the full error above or open the log file:"
    echo -e "${YELLOW}$(pwd)/${LOG_FILE}${NC}"
fi

echo -e ""
echo -e "${BLUE}====================================================${NC}"
echo -e "Press ENTER to close this window..."
read -r
