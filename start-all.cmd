@echo off
setlocal EnableExtensions
chcp 65001 >nul

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

echo Starting backend, frontend and mobile app...

start "community-service backend" cmd /k call "%PROJECT_ROOT%\scripts\start-backend.cmd"
timeout /t 2 /nobreak >nul
start "community-service frontend" cmd /k call "%PROJECT_ROOT%\scripts\start-frontend.cmd"
timeout /t 2 /nobreak >nul
start "community-service mobile" cmd /k call "%PROJECT_ROOT%\scripts\start-mobile.cmd"

echo Launch commands have been sent:
echo - Backend:  http://localhost:8080
echo - Frontend: http://localhost:7000
echo - Mobile:   http://localhost:9000
exit /b 0
