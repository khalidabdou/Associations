@echo off
title Associations - Windows MSI Packaging

echo ====================================================
echo        Associations - Windows MSI Packaging
echo ====================================================
echo.
echo Building Windows MSI installer...
echo This may take a few minutes, please wait...
echo.

call gradlew.bat :composeApp:packageReleaseMsi > build_windows_msi.log 2>&1

if %ERRORLEVEL% == 0 (
    echo.
    echo  SUCCESS! MSI installer created.
    echo.
    echo  Location:
    echo  %CD%\composeApp\build\compose\binaries\main\msi\
    echo.
    dir "composeApp\build\compose\binaries\main\msi\" 2>nul || echo Folder not found
    echo.
    echo  Opening the folder for you...
    explorer "composeApp\build\compose\binaries\main\msi\"
) else (
    echo.
    echo  BUILD FAILED!
    echo.
    echo  Error details saved in: %CD%\build_windows_msi.log
    echo  Opening log file...
    echo.
    type build_windows_msi.log
)

echo.
echo ====================================================
pause
