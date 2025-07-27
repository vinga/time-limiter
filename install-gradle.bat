@echo off
echo Installing Gradle and setting up wrapper...

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher first
    pause
    exit /b 1
)

echo Java is installed, continuing...

REM Check if we're in the right directory
if not exist "build.gradle" (
    echo ERROR: build.gradle not found
    echo Please run this script from the project root directory
    pause
    exit /b 1
)

REM Option 1: Try using chocolatey to install gradle
echo Checking if Chocolatey is available...
choco --version >nul 2>&1
if not errorlevel 1 (
    echo Installing Gradle via Chocolatey...
    choco install gradle -y
    if not errorlevel 1 (
        echo Gradle installed successfully via Chocolatey
        gradle wrapper --gradle-version 8.5
        goto :success
    )
)

REM Option 2: Download Gradle manually
echo Chocolatey not available, downloading Gradle manually...
set GRADLE_VERSION=8.5
set GRADLE_HOME=C:\gradle
set GRADLE_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip

if not exist "%GRADLE_HOME%" mkdir "%GRADLE_HOME%"

echo Downloading Gradle %GRADLE_VERSION%...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%GRADLE_URL%' -OutFile '%TEMP%\gradle.zip'}"

if exist "%TEMP%\gradle.zip" (
    echo Extracting Gradle...
    powershell -Command "Expand-Archive -Path '%TEMP%\gradle.zip' -DestinationPath '%GRADLE_HOME%' -Force"
    
    REM Add Gradle to PATH for this session
    set PATH=%GRADLE_HOME%\gradle-%GRADLE_VERSION%\bin;%PATH%
    
    echo Testing Gradle installation...
    gradle --version
    if not errorlevel 1 (
        echo Gradle installed successfully!
        echo Generating wrapper...
        gradle wrapper --gradle-version 8.5
        goto :success
    ) else (
        echo Gradle installation failed
        goto :error
    )
) else (
    echo Failed to download Gradle
    goto :error
)

:success
echo.
echo ================================
echo SUCCESS! Gradle wrapper is ready
echo ================================
echo You can now run:
echo   gradlew.bat build
echo   gradlew.bat run
echo.
goto :end

:error
echo.
echo ================================
echo ERROR: Setup failed
echo ================================
echo Please install Gradle manually:
echo 1. Download from: https://gradle.org/install/
echo 2. Extract to C:\gradle
echo 3. Add C:\gradle\gradle-8.5\bin to your PATH
echo 4. Run: gradle wrapper
echo.

:end
pause
