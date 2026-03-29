@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "JAVA_BIN="
if defined JAVA_HOME (
    set "JAVA_BIN=%JAVA_HOME%"
    if exist "!JAVA_BIN!\bin\java.exe" (
        set "JAVA_BIN=!JAVA_BIN!\bin\java.exe"
    ) else if exist "!JAVA_BIN!\java.exe" (
        set "JAVA_BIN=!JAVA_BIN!\java.exe"
    ) else (
        set "JAVA_BIN="
    )
)

if not defined JAVA_BIN (
    set "JAVA_BIN=java"
)

set "WINDOW_MODE=1"
if exist "config\Interface.ini" (
    for /f "usebackq tokens=* delims=" %%I in ("config\Interface.ini") do (
        set "LINE=%%I"
        set "LINE=!LINE: =!"
        if /i "!LINE!"=="EnableGUI=true" set "WINDOW_MODE=0"
    )
)

set "JAVA_CMD=%JAVA_BIN% -cp ./../libs/* org.l2jmobius.tools.DatabaseInstaller"

if "%WINDOW_MODE%"=="1" (
    start "L2J Mobius - Database Installer" %JAVA_CMD%
) else (
    %JAVA_CMD%
)

endlocal