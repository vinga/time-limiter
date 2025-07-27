@echo off
echo Diagnosing Gradle installation...
echo.

REM Check if gradle is in PATH
gradle --version >nul 2>&1
if not errorlevel 1 (
    echo ✅ Gradle is working!
    gradle --version
    echo.
    echo Running gradle wrapper...
    gradle wrapper --gradle-version 8.5
    goto :end
)

echo ❌ Gradle not found in PATH
echo.

REM Check common Chocolatey installation paths
echo Checking Chocolatey installation paths...

if exist "C:\ProgramData\chocolatey\lib\gradle\tools\gradle-*\bin\gradle.bat" (
    echo ✅ Found Gradle in Chocolatey tools directory
    for /d %%i in ("C:\ProgramData\chocolatey\lib\gradle\tools\gradle-*") do (
        echo Found Gradle at: %%i
        set "GRADLE_HOME=%%i"
        set "PATH=%%i\bin;%PATH%"
        echo Testing Gradle...
        "%%i\bin\gradle.bat" --version
        if not errorlevel 1 (
            echo ✅ Gradle is working from: %%i\bin
            echo.
            echo Generating wrapper...
            "%%i\bin\gradle.bat" wrapper --gradle-version 8.5
            goto :success
        )
    )
)

REM Check if gradle is in chocolatey bin
if exist "C:\ProgramData\chocolatey\bin\gradle.bat" (
    echo ✅ Found Gradle in Chocolatey bin directory
    "C:\ProgramData\chocolatey\bin\gradle.bat" --version
    if not errorlevel 1 (
        echo ✅ Gradle is working!
        echo.
        echo Generating wrapper...
        "C:\ProgramData\chocolatey\bin\gradle.bat" wrapper --gradle-version 8.5
        goto :success
    )
)

REM Manual PATH fix
echo.
echo 🔧 Manual fix required:
echo 1. Open a NEW PowerShell/Command Prompt as Administrator
echo 2. Run: refreshenv
echo 3. Then try: gradle --version
echo.
echo If that doesn't work:
echo 1. Check if Gradle was actually installed: choco list gradle
echo 2. If not installed, run: choco install gradle -y
echo 3. If installed but not working, run: choco uninstall gradle -y ^&^& choco install gradle -y
echo.
goto :end

:success
echo.
echo ✅ SUCCESS! Gradle wrapper generated successfully!
echo You can now run: gradlew.bat build
echo.

:end
pause
