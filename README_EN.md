# Bluetooth Disable

**Language:** [Russian](README.md) | [English](README_EN.md)

A minimal Android DPC for system-level Bluetooth blocking on a fully managed device.

## Status

Working V1 foundation:

- Android 8.0+ (`minSdk 26`);
- `targetSdk 36`;
- Kotlin + Jetpack Compose;
- Device Owner / Device Policy Controller operation;
- protection enabled through `UserManager.DISALLOW_BLUETOOTH`;
- protection disabled through `DevicePolicyManager.clearUserRestriction`;
- the effective Android Device Policy state is the source of truth;
- Android 12+ admin-integrated provisioning entry points;
- QR provisioning template for fully managed enrollment;
- no root, Shizuku, Magisk, Accessibility Service, or background service;
- no `INTERNET` permission or Bluetooth runtime permissions;
- no analytics or telemetry.

## Modes

### NORMAL

Bluetooth works normally.

### PROTECTED

The Device Owner applies `UserManager.DISALLOW_BLUETOOTH`. Android blocks Bluetooth system-wide until the restriction is removed.

## Build

```bash
./gradlew assembleDebug
```

Full local verification:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Device Owner for development

Installing the APK normally does not make the application the Device Owner.

On a test device without accounts, install the debug APK and then run:

```bash
adb shell dpm set-device-owner \
  com.pulse.bluetoothdisable/.admin.AppDeviceAdminReceiver
```

After that, the application can apply the system Bluetooth policy.

Details: [`docs/DEVELOPMENT_EN.md`](docs/DEVELOPMENT_EN.md).

## QR provisioning for users

Production deployment uses QR provisioning after a factory reset. The Android 12+ entry points are already implemented; the final QR code still requires a signed release APK, a stable public HTTPS URL, and the checksum of the exact APK.

Template and procedure: [`docs/QR_PROVISIONING_EN.md`](docs/QR_PROVISIONING_EN.md).

## Security

The release keystore, passwords, and signing credentials must never be stored in the repository.
