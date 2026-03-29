@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "BUILD_DIR=%ROOT_DIR%build"
set "ANT_LOG=%ROOT_DIR%ant.log"

if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

if exist "%ANT_LOG%" del /f /q "%ANT_LOG%"

echo ============================================================
echo  Building L2J_Mobius_Essence_9.2_RoseVain
echo ============================================================

where ant >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Apache Ant is not found in PATH.
    echo Please install Ant and add it to your PATH environment variable.
    pause
    exit /b 1
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not found in PATH.
    echo Please install JDK 25 and add it to your PATH environment variable.
    pause
    exit /b 1
)

ant -f "%ROOT_DIR%build.xml" assemble > "%ANT_LOG%" 2>&1

if %errorlevel% neq 0 (
    echo.
    echo BUILD FAILED.
    echo See log: "%ANT_LOG%"
    pause
    exit /b 1
)

echo.
echo BUILD SUCCESSFUL.
echo Log saved to: "%ANT_LOG%"
pause
endlocal
