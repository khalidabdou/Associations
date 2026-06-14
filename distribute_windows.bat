@echo off
title Associations - Windows MSI Packaging

echo ====================================================
echo        Associations - Windows MSI Packaging
echo ====================================================
echo.
echo Building Windows MSI installer...
echo This may take a few minutes, please wait...
echo.

call gradlew.bat :composeApp:packageMsi > build_windows_msi.log 2>&1

if %ERRORLEVEL% == 0 (
    echo.
    echo  SUCCESS! MSI installer created.
    echo.
    echo  Location:
    echo  %CD%\composeApp\build\compose\binaries\main\msi\
    echo.
    dir "composeApp\build\compose\binaries\main\msi\" 2>nul
    echo.
    echo  Opening the folder for you...
    explorer "composeApp\build\compose\binaries\main\msi\"
) else (
    echo.
    echo  BUILD FAILED! Full error below:
    echo.
    type build_windows_msi.log
    echo.
    echo  Log also saved at: %CD%\build_windows_msi.log
)

echo.
echo ====================================================
pause
