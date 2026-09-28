# Bluetooth Disable

**Language:** [Russian](README.md) | [English](README_EN.md)

Bluetooth Disable is an Android application that provides system-level Bluetooth blocking on a device.

Unlike simply turning Bluetooth off, protection mode uses Android Device Policy. While protection is active, Android prevents normal Bluetooth use until the restriction is removed.

## Features

- system-level Bluetooth blocking;
- immediate Bluetooth shutdown when protection is enabled;
- protection persists after closing the app and rebooting the device;
- simple manual `OFF / PROTECTED` mode;
- quick access through an Android Quick Settings tile;
- option to hide the app from the launcher;
- Russian and English interface;
- no root required;
- no Shizuku, Magisk, or Accessibility Service;
- no background service;
- no analytics or telemetry;
- no Internet access.

## How protection works

Bluetooth Disable operates as a **Device Policy Controller (DPC)** with **Device Owner** status.

When protection is enabled, the app applies the Android system restriction:

`UserManager.DISALLOW_BLUETOOTH`

Android then blocks Bluetooth use until the restriction is removed.

The protection state is stored by Android Device Policy and does not depend on whether the app process is running.

## Privacy

Bluetooth Disable follows a least-privilege approach.

The app:

- contains no ads;
- contains no analytics;
- contains no trackers;
- does not request the `INTERNET` permission;
- does not scan for Bluetooth devices;
- does not request location access;
- does not enumerate paired devices;
- does not connect to Bluetooth devices.

On Android 12 and later, `BLUETOOTH_CONNECT` is used to request immediate shutdown of the local Bluetooth adapter when protection is activated.

## Requirements

- Android 8.0 or later;
- the device must be configured with Bluetooth Disable as Device Owner.

Assigning Device Owner on a regular user device requires initial setup after a factory reset.

## Installation

Ready-to-use APKs are published in [Releases](../../releases).

For full protection functionality, Bluetooth Disable must be installed as Device Owner.

## Project status

Bluetooth Disable is under active development.

The core system-level Bluetooth blocking mechanism is implemented and has been tested on a physical Android device.

Support and behavior may vary depending on the device manufacturer and Android version.

## Technology

- Kotlin;
- Jetpack Compose;
- Android DevicePolicyManager;
- Android Enterprise / Device Owner;
- minSdk 26.
