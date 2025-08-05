@echo off
setlocal enabledelayedexpansion

REM ========================================
REM Enhanced App Time Limiter
REM Build and Install Script
REM ========================================

title Enhanced App Time Limiter - Build and Install

echo.
echo ========================================
echo Enhanced App Time Limiter
echo Build and Install Script
echo ========================================
echo.

REM Check if we're in the correct directory
if not exist "build.gradle" (
    echo ERROR: build.gradle not found!
    echo Please run this script from the project root directory
    pause
    exit /b 1
)

REM Check for administrator privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: Administrator privileges required!
    echo Please right-click this script and select "Run as administrator"
    pause
    exit /b 1
)

echo Step 1: Building application...
echo ================================

REM Build the fat JAR
call gradlew.bat fatJar
if errorlevel 1 (
    echo ERROR: Build failed!
    echo Please check the build output above for errors
    pause
    exit /b 1
)

echo.
echo ✓ Build completed successfully!

REM Check if JAR was created
set "JAR_FILE=build\libs\app-time-limiter-1.0.0-all.jar"
if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found at %JAR_FILE%
    pause
    exit /b 1
)

echo ✓ JAR file created: %JAR_FILE%

echo.
echo Step 2: Preparing for installation...
echo ====================================

REM Copy JAR to current directory for installer
copy "%JAR_FILE%" "app-time-limiter-1.0.0-all.jar" >nul
if errorlevel 1 (
    echo ERROR: Failed to copy JAR file for installation
    pause
    exit /b 1
)

echo ✓ JAR file prepared for installation

echo.
echo Step 3: Running installer...
echo ============================

REM Run the installer
call install.bat

echo.
echo ========================================
echo Build and Install Process Complete!
echo ========================================
echo.

REM Cleanup
del "app-time-limiter-1.0.0-all.jar" 2>nul

echo Build artifacts are available in the build\ directory
echo Installation logs are in the installation directory
echo.
pause