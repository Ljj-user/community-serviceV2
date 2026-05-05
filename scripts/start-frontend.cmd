@echo off
setlocal EnableExtensions
chcp 65001 >nul

set "PROJECT_ROOT=%~dp0.."
for %%I in ("%PROJECT_ROOT%") do set "PROJECT_ROOT=%%~fI"
set "FRONTEND_DIR=%PROJECT_ROOT%\frontend"

if not exist "%FRONTEND_DIR%" (
  echo frontend directory not found: %FRONTEND_DIR%
  exit /b 1
)

call :resolve_pnpm
if errorlevel 1 exit /b 1

call :stop_port 7000

echo PNPM=%PNPM_CMD%
cd /d "%FRONTEND_DIR%"
call "%PNPM_CMD%" run dev
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
