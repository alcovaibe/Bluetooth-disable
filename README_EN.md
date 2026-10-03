# Bluetooth Disable

**Language:** [Русский](README.md) | [English](README_EN.md)

Bluetooth Disable is an Android application that blocks Bluetooth at the system-policy level through Android Device Policy.

When protection is enabled and the application is provisioned as Device Owner, it applies `UserManager.DISALLOW_BLUETOOTH`. Android then prevents normal Bluetooth use until the restriction is removed. The application also performs a best-effort immediate adapter shutdown so the visible transition happens without unnecessary delay.

> **Project status:** the core Bluetooth functionality and all four Cover Modes are implemented. The project is currently in release-hardening and final-testing stage.

## Features

- system-level Bluetooth restriction through Device Owner;
- immediate best-effort Bluetooth shutdown when protection is enabled;
- policy remains active after the application is closed or the device is rebooted;
- simple `OFF / PROTECTED` state model;
- Android Quick Settings tile;
- launcher icon and application-name switching;
- four complete Cover Modes:
  - Calculator;
  - Notes;
  - Calendar;
  - Gallery;
- a separate hidden access mechanism for each cover;
- emergency recovery through Android system authentication;
- English and Russian interfaces;
- light and dark themes;
- no root, Shizuku, Magisk, or Accessibility Service requirement;
- no network background service;
- no analytics or telemetry;
- no `INTERNET` permission.

## Cover Modes

Cover Modes change both the launcher identity and the first screen shown by the application. They are functional local interfaces rather than icon-only disguises.

### Calculator

The user configures a five-digit access code. Entering the code in the calculator and pressing `=` opens Bluetooth Disable.

The plaintext code is not persisted. Verification uses HMAC-SHA256 with a non-exportable Android Keystore key.

If the code is forgotten, open calculator history and hold the **History** header for **3 seconds**, complete Android system authentication, and confirm the cover reset.

[Current Calculator Cover Mode documentation](docs/covermode/calculator/en/README.md)

### Notes

Notes Cover Mode provides local text notes, checklists, formatting, images, and favorites.

During setup, the user selects a note and a secret text range. Tapping that fragment inside the configured note opens Bluetooth Disable. The access rule is verified using HMAC-SHA256 with an Android Keystore key.

Emergency recovery starts by holding the `+` floating action button for **3 seconds**, followed by Android system authentication.

[Notes Cover Mode documentation](docs/covermode/notes/en/NOTES_COVER_MODE.md)

### Calendar

Calendar Cover Mode provides a local month calendar with date-bound notes.

During setup, a secret date and text are configured. To open Bluetooth Disable, create a note with that text on the configured date and open it. Leading and trailing whitespace is ignored; letter case and internal characters remain significant.

Emergency recovery starts by holding the calendar title or the control that returns to today's date for **3 seconds**.

[Calendar Cover Mode documentation](docs/covermode/calendar/en/CALENDAR_COVER_MODE.md)

### Gallery

Gallery Cover Mode is a local private gallery. Photos are imported through the Android Photo Picker and copied into application-private storage.

Hidden access uses one configured secret image and a sequence of three distinct screen zones. The access verifier uses HMAC-SHA256. Gallery files and metadata are protected locally with Android Keystore-backed encryption.

Emergency recovery starts by holding the **Gallery** title for **3 seconds**.

[Gallery Cover Mode documentation](docs/covermode/gallery/en/GALLERY_COVER_MODE.md)

## Emergency recovery

Recovery uses Android system authentication, including supported device credentials such as:

- biometrics;
- device PIN;
- pattern;
- password.

After successful authentication, the user must separately confirm the cover reset. Cancelling authentication or declining the confirmation leaves the current Cover Mode unchanged.

If the Quick Settings tile is already installed, Bluetooth Disable can also be opened from the tile.

## How Bluetooth protection works

The main protection path is:

```text
Device Owner
    ↓
DevicePolicyManager
    ↓
UserManager.DISALLOW_BLUETOOTH
    ↓
Android prevents Bluetooth use
```

After applying or clearing the restriction, the application reads the effective policy state again and confirms that the change actually took effect. Direct adapter shutdown is used only as a best-effort acceleration path.

## Privacy

Bluetooth Disable follows a minimal-permission design.

The application:

- contains no advertising;
- contains no analytics or trackers;
- does not request `INTERNET` permission;
- does not scan for Bluetooth devices;
- does not request location;
- does not enumerate paired Bluetooth devices;
- does not connect to remote Bluetooth devices;
- does not upload notes, photos, or other user content.

On Android 12+, `BLUETOOTH_CONNECT` is used only for the best-effort immediate shutdown of the local adapter. A Device Owner installation can grant that permission through Device Policy.

## Data storage and device transfer

Application data is excluded from Android backup and device-to-device transfer on supported Android versions.

`DeviceTransferGuard` adds another layer based on a random marker and a non-exportable Android Keystore HMAC key. If an OEM migration tool still copies application-private files to another device without the original Keystore key, local application state is reset to clean-install state.

## Requirements

- Android 8.0 (API 26) or newer;
- Device Owner provisioning is required for system-level Bluetooth blocking.

Normal Device Owner provisioning happens during Android initial setup and typically requires a factory-reset device.

## Device Owner QR installation

Stable QR for the latest published build:

![Device Owner QR](docs/bluetooth-disable-device-owner-qr.png)

Typical provisioning flow:

1. back up required data and factory-reset the device;
2. on the first Android Setup Wizard screen, enter QR provisioning mode — many devices require several taps on an empty area of the first setup screen;
3. connect to Wi-Fi when Setup Wizard requests it;
4. scan the QR above;
5. wait for Android to download and verify the APK;
6. complete Device Owner provisioning and normal initial setup;
7. open Bluetooth Disable and verify Device Owner status and Bluetooth protection.

Setup Wizard behavior can vary between Android OEMs.

[Device Owner QR provisioning documentation](docs/QR_PROVISIONING_EN.md)

## Releases

All new releases use one tag format:

```text
v<version>
```

For example: `v1.0.20`.

The release APK is named:

```text
BluetoothDisable-v<version>.apk
```

For each `v*` release, the workflow automatically:

1. builds and signs the release APK;
2. creates the GitHub Release;
3. calculates SHA-256 from the exact published APK;
4. generates Device Owner provisioning JSON and QR;
5. attaches QR and JSON to the Release;
6. updates the stable QR and provisioning JSON under `docs/` on `main`.

## Build and verification

The project uses Kotlin, Jetpack Compose, Material 3, Android Device Policy, and Android Keystore.

PR CI runs:

- unit tests;
- Android Lint;
- debug assembly;
- instrumentation-test APK assembly;
- minified signed test release assembly;
- instrumentation tests across Android API 26–36.

After merge, Android Full Compatibility runs the API 26–36 matrix again on `main`.

Production signing uses a separate release keystore supplied through GitHub Actions secrets or local `keystore.properties`.

## Feedback

Reproducible issues can be filed through GitHub Issues. Reports should include the device model, Android version, provisioning method, and exact reproduction steps when possible.
