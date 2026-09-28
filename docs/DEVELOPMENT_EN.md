# Development setup

**Language:** [Russian](DEVELOPMENT.md) | [English](DEVELOPMENT_EN.md)

## Requirements

- Android Studio / Android SDK 36;
- JDK 17;
- Android 8.0+ test device or a compatible emulator;
- ADB.

## 1. Build

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is produced by the standard Android Gradle Plugin in `app/build/outputs/apk/debug/`.

## 2. Install the APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

With a normal installation, the application should display `DEVICE OWNER REQUIRED`. This is expected behavior.

## 3. Assign Device Owner

For development testing, Google allows a custom DPC to be assigned through ADB when the device has no accounts and no conflicting owner or management profile.

```bash
adb shell dpm set-device-owner \
  com.pulse.bluetoothdisable/.admin.AppDeviceAdminReceiver
```

Check the current owner with:

```bash
adb shell dpm list-owners
```

The available `dpm` commands may vary slightly across Android versions. For the application itself, `DevicePolicyManager.isDeviceOwnerApp()` remains the source of truth.

## 4. Functional verification

After assigning Device Owner:

1. Launch the application.
2. Confirm that it displays `OFF`.
3. Enable Bluetooth manually in Android.
4. Tap the protection enable button.
5. Confirm that the application displays `PROTECTED` only after the restriction has been verified.
6. Verify that Android no longer allows normal Bluetooth use.
7. Close and reopen the application — the state should remain `PROTECTED`.
8. Reboot the device — the system policy should persist.
9. Tap the protection disable button.
10. Confirm that the application displays `OFF` and Bluetooth can be enabled manually again.

## 5. What the application does not do yet

The current version does not include:

- production-ready user QR provisioning;
- a user-facing deprovisioning flow;
- automatic re-enabling of Bluetooth after protection is disabled;
- network-based updates;
- analytics;
- Bluetooth profile management;
- a background service.

Do not use the debug build as the final provisioning mechanism on a primary personal device. Proper user onboarding and deprovisioning will be implemented separately.
