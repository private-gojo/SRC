# Relay SSH

Relay SSH is an independent Android SSH tunneling application and the Kotlin migration line of the original Second VPN source.

## Identity

- Application name: Relay SSH
- Application ID: `dev.relay.ssh`
- Version: `1.0` (`versionCode 1`)
- Primary language: Kotlin
- UI: Existing Android Views/XML during the migration

## Build configuration

- Android Gradle Plugin: 8.6.1
- Gradle wrapper: 8.7
- Kotlin Gradle Plugin: 2.1.10
- Compile SDK: 34
- Minimum SDK: 21
- Target SDK: 35
- Java and Kotlin bytecode target: 1.8

Build a debug APK from this directory:

```sh
./gradlew --no-daemon assembleDebug
```

## Migration status

This is an incremental Kotlin migration, not a wholesale replacement of the working tunneling stack.

The first migration step establishes a separate application ID and Kotlin launcher/application adapters while retaining the existing Java implementation behind them. The following utility classes have been translated to Kotlin with Java-callable APIs preserved:

- `SharedPref`
- `IpAddress`
- `StreamGobbler`
- `Lists`

The existing SSH2 implementation, HTTP/HTTPS payload handling, SOCKS forwarding, VPN service, tun2socks integration, JNI libraries, and third-party compatibility sources remain unchanged at this stage. They will be migrated or retained based on the component audit, not mechanically rewritten.

The original `SECONDVPN/` project is not modified by this migration.
