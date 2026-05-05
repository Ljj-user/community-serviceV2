@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "SQL_FILE=%SCRIPT_DIR%init_all.sql"

if not exist "%SQL_FILE%" (
  echo [ERROR] Init script not found: "%SQL_FILE%"
  exit /b 1
)

where mysql >nul 2>nul
if errorlevel 1 (
  echo [ERROR] mysql command not found. Please install MySQL client and add it to PATH.
  exit /b 1
)

echo [INFO] Running database init script with schema and temp data:
echo        %SQL_FILE%
echo.
mysql -u root -p < "%SQL_FILE%"

if errorlevel 1 (
  echo.
  echo [ERROR] Database initialization failed.
  exit /b 1
)

echo.
echo [OK] Database initialization completed.
endlocal
