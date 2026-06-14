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
echo -e "${BLUE}       Associations - Windows MSI Packaging          ${NC}"
echo -e "${BLUE}====================================================${NC}"

echo -e "Building Windows MSI installer..."
./gradlew :composeApp:packageReleaseMsi

MSI_PATH="composeApp/build/compose/binaries/main/msi"

echo -e ""
echo -e "${GREEN}✓ Done!${NC} MSI installer created."
echo -e "Location: ${YELLOW}$(pwd)/${MSI_PATH}${NC}"
echo -e ""
ls -l "${MSI_PATH}" 2>/dev/null || true
echo -e "${BLUE}====================================================${NC}"
