# Calendar Cover Mode

## Purpose

Calendar Cover Mode disguises Bluetooth Disable as a local calendar with date-bound notes. After activation, the launcher opens the Calendar interface while the main Bluetooth Disable UI is accessible through the configured date-and-note-text rule or emergency recovery.

## Features

The current implementation supports:

- month calendar view;
- navigation between months;
- date selection;
- quick return to today's date;
- creating, editing and deleting notes for specific dates;
- local note storage;
- light and dark themes.

## Hidden-access setup

1. Select Calendar Cover Mode and confirm the transition.
2. Choose the secret date in the setup screen.
3. Enter a secret text value between 3 and 100 characters.
4. Confirm the setup to persist the access rule and activate the cover.

The setup does not automatically create the secret calendar note. The user later creates a normal note on the configured date.

The configured date is bound into the signed payload together with the normalized text. The secret is not stored as a separate plaintext value. Verification uses HMAC-SHA256 with a non-exportable Android Keystore key.

## Opening Bluetooth Disable

To open the main interface:

1. navigate to the configured date;
2. create a note containing the configured text;
3. open that note.

Only leading and trailing whitespace is removed before verification. Letter case and all internal characters remain significant, so the rest of the text must match exactly.

The **HIDE** action returns to Calendar Cover Mode, moves the calendar back to today's date, and removes the main Bluetooth Disable screen from the navigation back stack.

## Emergency recovery

On the calendar screen, hold the calendar title or the control that returns to today for **3 seconds**. Android system authentication is requested. After successful authentication, the user must confirm the cover reset.

When the Quick Settings tile is installed, Bluetooth Disable can also be opened from that tile.

Cancelling system authentication or declining the confirmation leaves the active cover unchanged.

## Data storage

Calendar notes are stored locally in encrypted form. `CalendarNotesCipher` uses AES-GCM with an Android Keystore key. The hidden-access rule uses a separate HMAC-SHA256 key.

Application data is excluded from Android backup and device-to-device transfer. `DeviceTransferGuard` adds a separate non-exportable Keystore identity and returns migrated app-private state to clean-install state when copied data is detected on another device.

## Main components

- `CalendarCoverActivity` — cover container and recovery flow;
- `CalendarCoverSetupActivity` — secret date and text setup;
- `CalendarViewModel` — calendar and note state;
- `LocalCalendarNotesRepository` — local persistence;
- `CalendarNotesCipher` — encrypted note storage;
- `CalendarAccessManager` — HMAC verification for date and text;
- `CalendarAccessPolicy` — secret normalization and limits;
- `CalendarDraftStore` — draft persistence during recreation.

## Verification

Calendar Cover Mode is covered by unit and instrumentation tests for:

- access-text policy;
- calendar date behavior;
- note CRUD and encrypted persistence;
- draft restoration after activity recreation;
- setup and activation;
- cover/main navigation;
- recovery and authentication cancellation;
- the 3-second recovery gesture;
- compatibility behavior on slower legacy APIs.

Instrumentation tests run in the shared CI compatibility matrix for Android API 26–36.
