@echo off
where gradle >nul 2>nul
if %errorlevel% neq 0 (
  echo Gradle is required. Please install Gradle 8.x and re-run this command.
  exit /b 1
)
gradle %*
