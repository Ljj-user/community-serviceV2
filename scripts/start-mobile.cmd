@echo off
setlocal EnableExtensions
chcp 65001 >nul

set "PROJECT_ROOT=%~dp0.."
for %%I in ("%PROJECT_ROOT%") do set "PROJECT_ROOT=%%~fI"
set "MOBILE_WORKSPACE_DIR=%PROJECT_ROOT%\basic-main"
set "MOBILE_APP_DIR=%MOBILE_WORKSPACE_DIR%\apps\example-nut"

if not exist "%MOBILE_WORKSPACE_DIR%" (
  echo basic-main directory not found: %MOBILE_WORKSPACE_DIR%
  exit /b 1
)
if not exist "%MOBILE_APP_DIR%" (
  echo mobile app directory not found: %MOBILE_APP_DIR%
  exit /b 1
)

call :resolve_pnpm
if errorlevel 1 exit /b 1

call :stop_port 9000

echo PNPM=%PNPM_CMD%
cd /d "%MOBILE_WORKSPACE_DIR%"
call "%PNPM_CMD%" --dir "%MOBILE_APP_DIR%" run dev -- --host 0.0.0.0 --port 9000
exit /b %errorlevel%

:resolve_pnpm
if exist "%APPDATA%\npm\pnpm.cmd" (
  set "PNPM_CMD=%APPDATA%\npm\pnpm.cmd"
  goto :eof
)

for /f "delims=" %%I in ('where pnpm.cmd 2^>nul') do (
  set "PNPM_CMD=%%~fI"
  goto :pnpm_found
)

echo pnpm not found. Please install pnpm first.
exit /b 1

:pnpm_found
if exist "%PNPM_CMD%" goto :eof

echo pnpm not found. Please install pnpm first.
exit /b 1

:stop_port
set "TARGET_PORT=%~1"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":%TARGET_PORT% .*LISTENING"') do (
  if not "%%P"=="0" if not "%%P"=="4" (
    taskkill /PID %%P /F >nul 2>nul
  )
)
exit /b 0
