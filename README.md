# App Time Limiter

A Windows application that monitors and limits the usage time of specific applications (Minecraft and Chrome). The application provides voice warnings before reaching the time limit and automatically closes the applications when the limit is exceeded.

## Features

- **Process Monitoring**: Real-time monitoring of target applications (Minecraft, Chrome)
- **Time Tracking**: Accurate tracking of application usage time
- **Voice Warnings**: Spoken warnings 5 minutes before time limit (configurable)
- **Automatic Termination**: Closes applications when time limit is reached
- **System Tray Integration**: Runs in background with system tray icon
- **User-Friendly GUI**: Simple interface to configure time limits
- **Logging**: Comprehensive logging for debugging and monitoring

## Prerequisites

- **Java 17 or higher**
- **Windows 10/11** (uses Windows-specific APIs)
- **IntelliJ IDEA** (recommended) or any Java IDE

## Building and Running

### Using Gradle Wrapper (Recommended)

1. **Clone or download the project**
2. **Open terminal in project directory**
3. **Build the project:**
   ```bash
   ./gradlew build
   ```

4. **Run the application:**
   ```bash
   ./gradlew run
   ```

### Using IntelliJ IDEA

1. **Open IntelliJ IDEA**
2. **File → Open → Select the project directory**
3. **Wait for Gradle sync to complete**
4. **Run the main class:** `com.apptimer.AppTimeLimiterMain`

## Usage

1. **Start the application**
2. **Set time limits:**
   - Minecraft time limit (in minutes)
   - Chrome time limit (in minutes)
   - Warning time before limit (default: 5 minutes)

3. **Click "Start Monitoring"**
4. **The application will:**
   - Monitor for Minecraft and Chrome processes
   - Track usage time
   - Issue voice warnings before limits
   - Automatically close applications when limits are reached

5. **System tray:** Click the tray icon to show/hide the main window

## Configuration

### Monitored Applications
Currently monitors:
- `minecraft.exe` - Minecraft game
- `chrome.exe` - Google Chrome browser

### Voice Notifications
- Uses Windows built-in Text-to-Speech (SAPI)
- Falls back to system beeps if TTS is unavailable
- Warnings issued 5 minutes before time limit (configurable)

## Project Structure

```
src/main/java/com/apptimer/
├── AppTimeLimiterMain.java    # Main application class with GUI
├── ProcessMonitor.java        # Windows process monitoring using JNA
├── TimeTracker.java          # Time tracking and limit management
└── VoiceNotifier.java        # Voice notifications and alerts

src/main/resources/
└── logback.xml               # Logging configuration

build.gradle                  # Gradle build configuration
```

## Dependencies

- **JavaFX 21** - Modern UI framework
- **JNA 5.14.0** - Java Native Access for Windows APIs
- **Logback 1.4.14** - Logging framework
- **Jackson 2.16.0** - JSON processing (for future configuration files)

## Building Executable

### Create Fat JAR
```bash
./gradlew fatJar
```
The executable JAR will be in `build/libs/app-time-limiter-1.0.0-all.jar`

### Create Native Runtime (JLink)
```bash
./gradlew jlink
```
Creates a custom JRE with the application in `build/image/`

## Development

### Adding New Applications to Monitor
1. Edit `ProcessMonitor.java`
2. Add process name to `targetProcesses` set
3. Update `getAppDisplayName()` method for user-friendly names

### Customizing Voice Messages
1. Edit `VoiceNotifier.java`
2. Modify `sayWarning()` and `sayTimeUp()` methods

### Logging
- Application logs to console and `logs/app-time-limiter.log`
- Log level can be adjusted in `logback.xml`
- Development: Set `com.apptimer` logger to DEBUG level

## Troubleshooting

### Common Issues

1. **Application won't start**
   - Ensure Java 17+ is installed
   - Check that JavaFX modules are available

2. **Voice notifications not working**
   - Ensure Windows Speech API is available
   - Application will fall back to system beeps

3. **Process monitoring not working**
   - Ensure application is run with appropriate permissions
   - Check Windows security settings

4. **System tray icon not appearing**
   - Ensure system tray is enabled in Windows
   - Check Windows notification area settings

### Debug Mode
Run with debug logging:
```bash
./gradlew run --args="--debug"
```

## Future Enhancements

- [ ] Configuration file support (JSON)
- [ ] Multiple user profiles
- [ ] Web-based dashboard
- [ ] Application usage statistics
- [ ] Parental controls integration
- [ ] Support for more applications
- [ ] Schedule-based time limits
- [ ] Break reminders

## License

This project is for educational purposes. Ensure compliance with application licensing terms when monitoring software usage.
