@echo off
echo Setting up Gradle wrapper...

REM Create the necessary directory structure
if not exist "gradle\wrapper" mkdir gradle\wrapper

REM Download Gradle distribution (we'll use the wrapper script to do this)
echo Downloading gradle-wrapper.jar...

REM Use PowerShell to download the wrapper JAR
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'}"

if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo Gradle wrapper JAR downloaded successfully!
    echo Now you can run: gradlew.bat build
) else (
    echo Failed to download gradle-wrapper.jar
    echo Please install Gradle manually and run: gradle wrapper
)

pause
