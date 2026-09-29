# Bluetooth Disable

**Language:** [Russian](README.md) | [English](README_EN.md)

Bluetooth Disable is an Android application that allows Bluetooth to be blocked at the system level on a device.

Unlike simply turning Bluetooth off, protection mode uses Android Device Policy capabilities. While protection is active, Android prevents Bluetooth from being enabled and used through standard system controls.

## Features

- system-level Bluetooth blocking;
- immediate Bluetooth shutdown when protection is enabled;
- protection persists after the app is closed and the device is rebooted;
- simple manual `OFF / PROTECTED` mode;
- quick access through the Android Quick Settings tile;
- ability to hide the app from the app list;
- ability to change the launcher icon and application name;
- calculator cover mode with a functional calculator interface;
- access to the main Bluetooth Disable interface through a five-digit code;
- ability to quickly return from the main interface to cover mode;
- Russian and English interface;
- works without root;
- works without Shizuku, Magisk, or Accessibility Service;
- no background service;
- no analytics or telemetry;
- no Internet access.

## App cover modes

Bluetooth Disable supports changing its launcher icon and application name.

The following variants are available:

- Default;
- Calculator;
- Notes;
- Calendar;
- Gallery.

### Calculator mode

The Calculator variant provides a functional cover mode.

When enabled, the application appears and launches as a regular calculator. During initial setup, the user creates a custom five-digit access code.

The calculator supports:

- addition, subtraction, multiplication, and division;
- percentages;
- parentheses;
- negative and decimal numbers;
- calculation history;
- light and dark themes.

To open the main Bluetooth Disable interface, enter the configured five-digit code in the calculator and press `=`.

After entering Bluetooth Disable, a **“HIDE”** button is available to return directly to the calculator interface.

The access code is stored locally only. The code itself is not stored in plaintext: verification uses HMAC-SHA256 with a key stored in Android Keystore.

The Notes, Calendar, and Gallery variants currently change the launcher icon and application name. Full cover interfaces for these variants are planned for future versions.

## How protection works

Bluetooth Disable operates as a **Device Policy Controller (DPC)** and receives **Device Owner** status.

When protection is enabled, the application applies the Android system restriction:

`UserManager.DISALLOW_BLUETOOTH`

After that, the operating system itself prevents Bluetooth from being used until the restriction is removed.

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
- does not enumerate paired devices;
- does not connect to Bluetooth devices.

On Android 12 and later, the `BLUETOOTH_CONNECT` permission is used to immediately turn off the local Bluetooth adapter when protection is activated.

## Requirements

- Android 8.0 or later;
- the device must be configured with Bluetooth Disable as Device Owner.

Assigning Device Owner on a regular user device requires initial setup after a factory reset.

## Installation

Ready-to-use APKs are published in [Releases](../../releases).

[Install via QR](#install-via-qr-code)

For system-level Bluetooth blocking, the application must be configured once as the **device owner (Device Owner)**. This is done during the initial Android setup process.

### Before you begin

> **Back up important data.** Configuring Device Owner requires a factory reset. Photos, files, applications, and other local data will be deleted.

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
   Internet access is needed by the system only at the stage of downloading and verifying the installation APK.

4. **Wait for the built-in QR scanner to appear.**
   Then scan the Bluetooth Disable QR code.

5. **Wait for device provisioning to finish.**
   Android will automatically download the application, verify it, and assign Bluetooth Disable as the device owner. At this point, you do not need to install anything manually.

6. **Do not interrupt the process.**
   While provisioning is in progress, do not turn off or restart the device. On some smartphones, this stage may take several minutes.

7. **Continue the normal Android setup.**
   When the system reports that provisioning is complete, tap **“Continue”** and finish the initial device setup as usual.

8. **Open Bluetooth Disable.**
   If setup completed successfully, the application will automatically detect Device Owner status and system-level Bluetooth protection mode will become available.

> Button names and screen appearance may differ slightly depending on the device manufacturer and Android version. The setup principle itself remains the same.

## Project status

Bluetooth Disable is under active development.

The core system-level Bluetooth blocking mechanism has already been implemented and tested on a physical Android device.

Support and behavior may vary depending on the device manufacturer and Android version.

## Technology

- Kotlin;
- Jetpack Compose;
- Android DevicePolicyManager;
- Android Enterprise / Device Owner;
- minSdk 26.

## Install via QR code

Scan this QR code during the initial Android setup to install Bluetooth Disable 1.0.6 as Device Owner.

![QR code for installing Bluetooth Disable 1.0.6](docs/bluetooth-disable-1.0.6-device-owner-qr.svg)
