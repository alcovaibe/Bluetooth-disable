# Bluetooth Disable

**Language:** [Russian](README.md) | [English](README_EN.md)

Bluetooth Disable is an Android application that allows Bluetooth to be blocked at the system level on a device.

Unlike simply turning Bluetooth off, protection mode uses Android Device Policy capabilities. While protection is active, Android prevents Bluetooth from being enabled and used through standard system controls.

> **Project status:** the core functionality and all cover modes are implemented. Final testing, UI polish, and preparation for the upcoming release are currently in progress.

## Features

- system-level Bluetooth blocking;
- immediate Bluetooth shutdown when protection is enabled;
- protection persists after the app is closed and the device is rebooted;
- simple manual `OFF / PROTECTED` mode;
- quick access through the Android Quick Settings tile;
- ability to change the launcher icon and application name;
- four complete cover modes:
  - Calculator;
  - Notes;
  - Calendar;
  - Gallery;
- a dedicated way to access the main Bluetooth Disable interface from each cover mode;
- emergency recovery through Android system authentication;
- ability to quickly return from the main interface to the active cover mode;
- Russian and English interface;
- light and dark themes;
- works without root;
- works without Shizuku, Magisk, or Accessibility Service;
- no background service;
- no analytics or telemetry;
- no Internet access for the application.

## Cover modes

Bluetooth Disable supports changing its icon, name, and startup interface.

The following variants are available:

- Default;
- Calculator;
- Notes;
- Calendar;
- Gallery.

All four cover modes have their own functional interfaces and are not limited to changing the app icon visually.

Cover-mode data is stored locally on the device.

---

### Calculator

When this mode is enabled, the application appears and launches as a regular calculator.

During initial setup, the user creates a custom five-digit access code.

The calculator supports:

- addition, subtraction, multiplication, and division;
- percentages;
- parentheses;
- negative and decimal numbers;
- recent calculation history;
- light and dark themes.

To open the main Bluetooth Disable interface, enter the configured five-digit code and press `=`.

After entering Bluetooth Disable, a **HIDE** button is available to return to the calculator interface.

If the code is forgotten and the Quick Settings tile has not been added, emergency recovery can be used:

1. open calculator history;
2. hold the **History** title for 7 seconds;
3. complete Android system authentication;
4. confirm the cover reset.

The application then returns to the default mode.

The access code is stored locally only. The code itself is not stored in plaintext: verification uses HMAC-SHA256 with a key from Android Keystore.

---

### Notes

Notes mode provides a complete local interface for working with notes.

It supports:

- creating notes;
- editing directly on the note screen;
- deleting notes;
- adding notes to favorites;
- a dedicated notes list;
- generating a note through a contextual action;
- local data storage;
- light and dark themes.

A normal tap on the `+` button creates a new note.

Holding the `+` button for 3 seconds opens the additional **Generate** action.

While viewing a note, the additional action menu is opened by tapping an empty area of the screen.

Notes mode also provides a protected way to return to the main Bluetooth Disable interface and emergency recovery through Android system authentication.

---

### Calendar

Calendar mode launches the application as a local calendar.

It supports:

- month-by-month calendar view;
- moving between months;
- selecting a date;
- creating notes for specific dates;
- editing and deleting notes;
- returning to today's date;
- local data storage;
- light and dark themes.

During initial setup, the user specifies a special date and note text used to access the main interface.

To open Bluetooth Disable, create a note with the configured text on the configured date and tap it.

Matching is case-sensitive; leading and trailing whitespace is ignored.

The **HIDE** button returns the calendar to today's date and removes the main Bluetooth Disable interface from the back stack.

Calendar notes are stored locally in encrypted form.

[Calendar Cover Mode architecture and verification](docs/CALENDAR_COVER_MODE.md).

---

### Gallery

Gallery mode provides a local private gallery.

Photos are imported through the Android system Photo Picker and copied into the application's internal storage.

It supports:

- photo import;
- displaying photos grouped by day;
- **Today**, **Yesterday**, and specific-date groups;
- full-screen viewing;
- swiping left and right between photos;
- zooming;
- adding photos to favorites;
- an automatic **Favorites** album;
- creating custom albums;
- adding photos to albums;
- deleting photos;
- rotating images;
- cropping images;
- viewing photo information.

The **Favorites** album is displayed only when at least one photo is marked as a favorite.

After a photo has been copied into the application's internal storage, the imported copy no longer depends on the original file.

Gallery data is stored locally. Photos, thumbnails, and service data are protected using Android Keystore and AES-256-GCM encryption.

The gallery does not provide a feature for uploading photos to the Internet.

---

## Emergency cover recovery

Cover modes include an access-recovery mechanism.

It uses Android system authentication:

- biometrics;
- device PIN;
- pattern;
- device password.

After successful authentication, the user can reset the active cover mode and return Bluetooth Disable to its default icon and interface.

Local user data for modes where it is applicable is retained when leaving a cover mode normally.

## How Bluetooth protection works

Bluetooth Disable operates as a **Device Policy Controller (DPC)** and receives **Device Owner** status.

When protection is enabled, the application applies the Android system restriction:

`UserManager.DISALLOW_BLUETOOTH`

After that, the operating system itself prevents Bluetooth from being used until the restriction is removed.

In addition, the application attempts to immediately turn off the active Bluetooth adapter so the visible state changes without delay.

The protection state is stored at the Android Device Policy level and does not depend on whether the application is running.

## Privacy

Bluetooth Disable is designed around the principle of minimal permissions.

The application:

- contains no ads;
- contains no analytics;
- contains no trackers;
- does not have the `INTERNET` permission;
- does not scan for Bluetooth devices;
- does not access location;
- does not enumerate paired Bluetooth devices;
- does not connect to remote Bluetooth devices;
- does not send notes, photos, or other user data to a server.

On Android 12 and later, the `BLUETOOTH_CONNECT` permission is used to immediately turn off the local Bluetooth adapter when protection is activated.

## Requirements

- Android 8.0 or later;
- the device must be configured with Bluetooth Disable as Device Owner to use system-level Bluetooth blocking.

Assigning Device Owner on a regular user device requires initial setup after a device reset.

## Installation

Ready-to-use builds are published in [Releases](../../releases).

[Install via QR](#install-via-qr-code)

For system-level Bluetooth blocking, the application must be configured once as the **device owner (Device Owner)**. This is done during the initial Android setup process.

### Before you begin

> **Back up important data.** Initial Device Owner configuration may require a factory reset. Photos, files, applications, and other local data will be deleted.

Prepare the following in advance:

- a backup of the data you need;
- access to Wi-Fi;
- the Bluetooth Disable QR code opened on another phone, tablet, or computer.

### Setting up Bluetooth Disable

1. **Reset the device to factory settings.**  
   Wait for the device to restart and for the first Android setup screen to appear.

2. **Open QR-code provisioning mode.**  
   On the first setup screen, tap an empty area of the screen several times in succession — usually **6 times**. Android will open the special device provisioning mode.

3. **Connect to Wi-Fi if Android asks you to.**  
   Internet access is required by the system while downloading and verifying the installation APK. Bluetooth Disable itself does not have the `INTERNET` permission.

4. **Wait for the built-in QR scanner to appear.**  
   Then scan the Bluetooth Disable QR code.

5. **Wait for device provisioning to finish.**  
   Android will automatically download the application, verify it, and assign Bluetooth Disable as the device owner.

6. **Do not interrupt the process.**  
   While provisioning is in progress, do not turn off or restart the device.

7. **Continue the normal Android setup.**  
   When the system reports that provisioning is complete, finish the initial device setup.

8. **Open Bluetooth Disable.**  
   If provisioning completed successfully, the application will automatically detect Device Owner status and system-level Bluetooth protection mode will become available.

> Button names, QR provisioning availability, and screen appearance may differ depending on the device manufacturer, Android version, and manufacturer system policies.

## Project status

Bluetooth Disable is under development. The core features and capabilities have already been implemented.

After version **1.1.0** is published, the application will move into a maintenance, optimization, and bug-fixing phase. Active development of new features and capabilities is not planned after the **1.1.0** release.

Currently implemented:

- the core system-level Bluetooth blocking mechanism;
- protection-state management;
- Quick Settings Tile;
- Device Owner operation;
- state recovery after reboot;
- launcher identity switching;
- complete Calculator cover mode;
- complete Notes cover mode;
- complete Calendar cover mode;
- complete Gallery cover mode;
- emergency recovery for cover modes;
- local protected storage for sensitive data;
- light and dark themes;
- edge-to-edge interface;
- Android 8.0–16 support.

Currently in progress:

- final testing across different Android versions and devices from different manufacturers;
- checking launcher-icon and splash-screen behavior;
- fixing remaining UI details;
- testing update and recovery scenarios;
- preparing the release build and documentation.

**The upcoming release is being prepared.**

## Compatibility

Minimum supported version:

- Android 8.0 / API 26.

Target Android version:

- Android 16 / API 36.

Device Policy behavior and some system UI elements may differ depending on the device manufacturer.

## Technology

- Kotlin;
- Jetpack Compose;
- Material 3;
- Android DevicePolicyManager;
- Android Enterprise / Device Owner;
- Android Keystore;
- AES-256-GCM;
- minSdk 26;
- targetSdk 36.

## Testing

The project uses automated GitHub Actions checks.

The following are checked:

- project build;
- unit tests;
- Android Lint;
- instrumented tests;
- compatibility with supported Android versions.

The full compatibility workflow checks API 26–36.

The application is also tested on physical Android devices.

## Install via QR code

The repository currently retains the QR code for the previous Bluetooth Disable 1.0.6 build:

![QR code for installing Bluetooth Disable 1.0.6](docs/bluetooth-disable-1.0.6-device-owner-qr.png)

The installation QR code will be updated before the next public release.
