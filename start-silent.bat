@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "LOGIN_DIR=%ROOT_DIR%build\dist\login"
set "GAME_DIR=%ROOT_DIR%build\dist\game"

if not exist "%LOGIN_DIR%\LoginServer.vbs" (
    echo [ERROR] LoginServer script not found: "%LOGIN_DIR%\LoginServer.vbs"
    pause
    exit /b 1
)

if not exist "%GAME_DIR%\GameServer.vbs" (
    echo [ERROR] GameServer script not found: "%GAME_DIR%\GameServer.vbs"
    pause
    exit /b 1
)

echo Starting LoginServer and GameServer in hidden background mode...
powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -Command "Start-Process wscript.exe -ArgumentList '//B //Nologo ""%LOGIN_DIR%\LoginServer.vbs""' -WorkingDirectory '%LOGIN_DIR%' -WindowStyle Hidden"
powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -Command "Start-Process wscript.exe -ArgumentList '//B //Nologo ""%GAME_DIR%\GameServer.vbs""' -WorkingDirectory '%GAME_DIR%' -WindowStyle Hidden"

echo Both start commands were sent.
echo Check logs in build\dist\login and build\dist\game.

exit /b 0
