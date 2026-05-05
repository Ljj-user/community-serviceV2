@echo off
setlocal EnableExtensions
chcp 65001 >nul

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
set "INIT_SQL=%PROJECT_ROOT%\backend\src\main\resources\db\init_all.sql"

if not exist "%INIT_SQL%" (
  echo Init SQL not found: "%INIT_SQL%"
  exit /b 1
)

where mysql >nul 2>nul
if errorlevel 1 (
  echo mysql command not found. Please install MySQL client and add it to PATH.
  exit /b 1
)

echo Initializing database community_service...
echo mysql will ask for the root password.
mysql -u root -p < "%INIT_SQL%"
if errorlevel 1 (
  echo Database initialization failed.
  exit /b 1
)

echo Database initialized successfully.
exit /b 0
