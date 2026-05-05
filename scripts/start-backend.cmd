@echo off
setlocal EnableExtensions
chcp 65001 >nul

set "PROJECT_ROOT=%~dp0.."
for %%I in ("%PROJECT_ROOT%") do set "PROJECT_ROOT=%%~fI"
set "BACKEND_DIR=%PROJECT_ROOT%\backend"
set "MAVEN_CMD=%PROJECT_ROOT%\tools\apache-maven-3.9.9\bin\mvn.cmd"
set "MAVEN_REPO_DIR=%PROJECT_ROOT%\.m2\repository"

if not exist "%BACKEND_DIR%" (
  echo backend directory not found: %BACKEND_DIR%
  exit /b 1
)
if not exist "%MAVEN_CMD%" (
  echo Maven not found: %MAVEN_CMD%
  exit /b 1
)
if not exist "%PROJECT_ROOT%\.m2" mkdir "%PROJECT_ROOT%\.m2"
if not exist "%MAVEN_REPO_DIR%" mkdir "%MAVEN_REPO_DIR%"

call :resolve_java_home
if errorlevel 1 goto :fail

call :stop_port 8080

echo JAVA_HOME=%JAVA_HOME%
echo MAVEN_REPO=%MAVEN_REPO_DIR%
cd /d "%BACKEND_DIR%"
call "%MAVEN_CMD%" -Dmaven.repo.local="%MAVEN_REPO_DIR%" -f "%BACKEND_DIR%\pom.xml" spring-boot:run
if errorlevel 1 goto :fail
exit /b 0

:resolve_java_home
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto :eof

for /f "delims=" %%I in ('where java 2^>nul') do (
  set "JAVA_EXE=%%~fI"
  goto :java_found
)

echo JDK not found. Please install JDK 17 and set JAVA_HOME.
exit /b 1

:java_found
for %%I in ("%JAVA_EXE%\..\..") do set "JAVA_HOME=%%~fI"
if exist "%JAVA_HOME%\bin\java.exe" goto :eof

echo Failed to resolve JAVA_HOME from: %JAVA_EXE%
exit /b 1

:stop_port
set "TARGET_PORT=%~1"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":%TARGET_PORT% .*LISTENING"') do (
  if not "%%P"=="0" if not "%%P"=="4" (
    taskkill /PID %%P /F >nul 2>nul
  )
)
exit /b 0

:fail
echo.
echo Backend start failed. Press any key to close this window.
pause >nul
exit /b 1
