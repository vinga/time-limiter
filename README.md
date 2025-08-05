1# Enhanced App Time Limiter v2.0

An advanced Windows application that monitors and limits the usage time of specific applications (Minecraft and Chrome) with enhanced security features, configurable block delays, and professional UI organization.

## 🆕 Enhanced Features v2.0

### 🔒 **Advanced Security System**
- **Failed attempt tracking** with automatic lockout after 5 attempts
- **5-minute security lockout** to prevent brute force attacks
- **Voice alerts** for security violations and unauthorized access
- **Enhanced keyboard shortcut blocking** (Alt+F4, Ctrl+Q, Ctrl+W)
- **Forced window closure protection** with authentication requirement
- **Authentication timeout** (30 minutes) with automatic logout

### ⏳ **Configurable Block Delays**
- **Custom block duration** per application after time limits exceeded
- **Default delays**: 60 minutes for both Minecraft and Chrome
- **Range**: 5 minutes to 24 hours (1440 minutes)
- **Automatic blocking** when time limits are reached
- **Manual blocking** with administrator authentication
- **Block time remaining** indicators and notifications

### 📊 **Enhanced UI Organization**
- **Color-coded sections** for better visual organization
- **Separate control and monitoring panels** for improved workflow
- **Real-time status indicators** for applications and security
- **Comprehensive activity logging** with timestamps
- **Organized configuration sections**:
  - Time Limits Configuration
  - Block Delays Configuration
  - Manual Blocking Controls
  - System Controls
  - Status Monitoring

### 🔊 **Advanced Voice Notifications**
- **Block expiration announcements** when apps become available
- **Security violation alerts** for unauthorized access attempts
- **Monitoring start/stop notifications**
- **Manual block/unblock announcements**
- **Enhanced warning messages** with remaining time

### 💾 **Persistent Settings Management**
- **All settings saved automatically** every 2 minutes
- **Daily time usage tracking** with automatic reset
- **Block delay configurations** persistence
- **Security preferences** and password hash storage
- **JSON-based configuration** for easy backup

## Core Features

### 🎯 **Application Monitoring**
- **Real-time monitoring** of target applications (Minecraft, Chrome)
- **Accurate time tracking** with 2-second precision
- **Automatic process detection** using Windows APIs
- **Aggressive process termination** for blocked applications

### 🔐 **Security Features**
- **Password protection** for all administrative functions
- **Default password**: `admin123` ⚠️ **CHANGE IMMEDIATELY!**
- **System tray integration** with protected access
- **Enhanced protection mode** preventing unauthorized closure

### 🔊 **Voice Notifications**
- **Spoken warnings** 5 minutes before time limit (configurable)
- **Windows Text-to-Speech integration** with fallback to system beeps
- **Multiple notification types** for different events

### 📝 **Comprehensive Logging**
- **Real-time activity log** with all events
- **Security event logging** for audit trails
- **Debug logging** for troubleshooting

## Installation & Setup

### Prerequisites
- **Java 17 or higher**
- **Windows 10/11** (uses Windows-specific APIs)
- **IntelliJ IDEA** (recommended) or any Java IDE

### Building and Running

#### Using Gradle Wrapper (Recommended)
```bash
# Clone or download the project
git clone <repository-url>
cd app-time-limiter

# Build the enhanced application
./gradlew build

# Run the enhanced application
./gradlew run
```

#### Using IntelliJ IDEA
1. **Open IntelliJ IDEA**
2. **File → Open → Select the project directory**
3. **Wait for Gradle sync to complete**
4. **Run the main class:** `com.apptimer.AppTimeLimiterMain`

## Usage Guide

### 🚀 **Initial Setup**
1. **Start the application** - monitoring begins automatically
2. **Change the default password** immediately:
   - Use menu: `Security → Change Password`
   - Choose a strong password (8+ characters)
3. **Configure time limits** (requires admin authentication):
   - Click "Edit Settings" button
   - Set Minecraft and Chrome limits (1-600 minutes)
   - Set warning time (1-30 minutes)
   - Configure block delays (5-1440 minutes)

### 📊 **Monitoring Operation**
- **Auto-start monitoring** with saved/default settings
- **Real-time status display** showing used/remaining time
- **Automatic warnings** before time limits
- **Automatic blocking** when limits exceeded
- **System tray operation** with right-click menu access

### 🛡️ **Security Operations**
- **Authentication required** for:
  - Modifying settings and block delays
  - Manual blocking/unblocking applications
  - Stopping monitoring
  - Exiting the application
- **No authentication required** for:
  - Restarting monitoring
  - Viewing status information
  - Hiding to system tray

### 🚫 **Manual Blocking**
1. **Access Manual Blocking section**
2. **Enter block duration** (1-1440 minutes)
3. **Click block button** (requires admin authentication)
4. **Application immediately terminated** and blocked

## Configuration

### Default Settings
- **Minecraft time limit**: 75 minutes
- **Chrome time limit**: 75 minutes
- **Warning time**: 5 minutes before limit
- **Minecraft block delay**: 60 minutes after limit
- **Chrome block delay**: 60 minutes after limit

### Settings File
- **Location**: `app-time-limiter-settings.json`
- **Auto-save**: Every 2 minutes during operation
- **Daily reset**: Time usage resets at midnight
- **Backup recommended**: Copy settings file for backup

## Enhanced Project Structure

```
src/main/java/com/apptimer/
├── AppTimeLimiterMain.java          # Enhanced main application with organized UI
├── ProcessMonitor.java              # Enhanced process monitoring with better blocking
├── ApplicationBlocker.java          # NEW: Configurable application blocking system
├── TimeTracker.java                 # Time tracking and limit management
├── SecurityManager.java             # Enhanced security with lockout protection
└── VoiceNotifier.java              # Enhanced voice notifications system

src/main/resources/
└── logback.xml                     # Logging configuration

build.gradle                        # Gradle build configuration with dependencies
app-time-limiter-settings.json     # Enhanced settings file (auto-generated)
```

## Enhanced Dependencies

- **JavaFX 21** - Modern UI framework for enhanced interface
- **JNA 5.14.0** - Java Native Access for Windows APIs
- **Logback 1.4.14** - Comprehensive logging framework
- **Jackson 2.16.0** - JSON processing for enhanced settings

## Building Distribution

### Create Enhanced Fat JAR
```bash
./gradlew fatJar
```
**Output**: `build/libs/app-time-limiter-1.0.0-all.jar`

### Create Native Runtime (JLink)
```bash
./gradlew jlink
```
**Output**: Custom JRE in `build/image/`

## Security Best Practices

### 🔐 **Password Security**
1. **Change default password immediately** upon first use
2. **Use strong passwords** (8+ characters, mixed case, numbers)
3. **Don't share passwords** with unauthorized users
4. **Regular password changes** for enhanced security

### 🛡️ **System Security**
- **Run with appropriate permissions** for process monitoring
- **Monitor security logs** for unauthorized access attempts
- **Regular backups** of settings and configurations
- **Keep system updated** for security patches

## Troubleshooting

### Common Issues

#### Application Won't Start
- **Verify Java 17+** is installed and in PATH
- **Check JavaFX modules** are available
- **Run from command line** to see error messages

#### Voice Notifications Not Working
- **Check Windows Speech API** availability
- **Verify audio drivers** are installed
- **Application falls back** to system beeps automatically

#### Process Monitoring Issues
- **Run as administrator** if permission errors occur
- **Check Windows security settings** for process access
- **Verify target applications** are spelled correctly

#### System Tray Problems
- **Enable system tray** in Windows settings
- **Check notification area settings** in taskbar
- **Restart application** if tray icon doesn't appear

#### Security Lockout Recovery
- **Wait 5 minutes** for automatic lockout expiration
- **Check logs** for lockout status and timing
- **Restart application** if lockout persists (last resort)

### Debug Mode
```bash
# Run with enhanced logging
./gradlew run --args="--debug"

# Check log files
tail -f logs/app-time-limiter.log
```

## Future Enhancements

### Planned Features
- [ ] **Multiple user profiles** with individual settings
- [ ] **Web-based dashboard** for remote monitoring
- [ ] **Application usage statistics** and reporting
- [ ] **Schedule-based time limits** (weekday/weekend)
- [ ] **Break reminders** and healthy usage patterns
- [ ] **Parental controls integration** with Windows
- [ ] **Additional application support** (games, browsers)
- [ ] **Cloud settings synchronization**
- [ ] **Mobile app companion** for monitoring
- [ ] **Advanced reporting** with charts and analytics

## Support & Contribution

### Getting Help
- **Check logs** in `logs/app-time-limiter.log` for error details
- **Review security status** using menu options
- **Test voice notifications** using Tools menu
- **Verify settings** are saved correctly

### Contributing
- **Report bugs** with detailed reproduction steps
- **Suggest features** for future enhancements
- **Submit pull requests** with improvements
- **Test on different** Windows versions

## License & Legal

This enhanced project is for **educational and legitimate parental control purposes**. 

**Important Legal Notes**:
- ✅ **Ensure compliance** with application licensing terms
- ✅ **Respect privacy rights** of monitored users
- ✅ **Use for legitimate purposes** only (parental controls, self-regulation)
- ⚠️ **Obtain appropriate consent** before monitoring
- ⚠️ **Follow local laws** regarding computer monitoring

---

## 🎯 Quick Start Checklist

1. ✅ **Install Java 17+**
2. ✅ **Build with `./gradlew build`**
3. ✅ **Run with `./gradlew run`**
4. ✅ **Change default password immediately**
5. ✅ **Configure time limits and block delays**
6. ✅ **Test voice notifications**
7. ✅ **Verify system tray integration**
8. ✅ **Check monitoring is active**

**Enhanced App Time Limiter v2.0** - Professional-grade application time management with advanced security and user experience enhancements.
