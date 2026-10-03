# Notes Cover Mode

## Purpose

Notes Cover Mode disguises Bluetooth Disable as a local notes application. After activation, the launcher opens the Notes interface while the main Bluetooth Disable UI is available only through the configured hidden note fragment or emergency recovery.

## Features

The current implementation supports:

- regular text notes;
- checklists;
- note creation, editing and deletion;
- favorites;
- rich-text formatting;
- embedded images;
- local sample-note generation;
- local-only storage with no network synchronization;
- light and dark themes.

## Hidden-access setup

1. Select Notes Cover Mode and confirm the transition.
2. In the setup screen, create a note, edit an existing note, or generate local notes.
3. Select one note to act as the access note.
4. Select a secret text range of up to 80 characters inside that note.
5. Confirm the setup and activate Notes Cover Mode.

The access rule stores the note identifier and the selected character range. The secret fragment is not stored as a separate plaintext value. Verification uses HMAC-SHA256 with a non-exportable key held in Android Keystore.

## Opening Bluetooth Disable

Open the configured note and tap inside the selected secret fragment. Access is granted only when all of the following are true:

- the configured note is open;
- the tap is inside the saved character range;
- the current text in that range still matches the configured content;
- the HMAC verifies with the Android Keystore key.

Editing or deleting the secret note displays an additional warning to reduce the chance of accidentally destroying the configured access path.

## Emergency recovery

On the Notes list screen, hold the `+` floating action button for **3 seconds**. Android system authentication is then requested. After successful authentication, the user must confirm the cover reset.

When the Quick Settings tile is already installed, the application can also be opened from that tile.

Cancelling authentication or declining the reset leaves the active cover unchanged.

## Data storage

Notes are stored locally only. The notes repository uses AES-256-GCM and stores its encryption key in Android Keystore. The data-encryption key is separate from the HMAC key used for hidden access.

Application data is excluded from Android backup and device-to-device transfer. `DeviceTransferGuard` adds another installation-identity check using a separate non-exportable Keystore key and resets migrated local state when copied app data is detected on another device.

## Main components

- `NotesCoverActivity` — cover container and recovery flow;
- `NotesCoverSetupActivity` — note and secret-range setup;
- `NotesViewModel` — UI state and note operations;
- `LocalNotesRepository` — local persistence;
- `NotesCipher` — AES-256-GCM note-data encryption;
- `NotesAccessManager` — HMAC verification for the hidden fragment;
- `NotesPolicy` — validation and limits;
- `NotesGenerator` — local note generation.

## Verification

The mode has unit and instrumentation coverage for:

- note generation;
- local repository persistence;
- state persistence across recreation;
- navigation between Notes Cover Mode and Bluetooth Disable;
- recovery hold behavior;
- hidden-access behavior.

Instrumentation tests run in the shared CI compatibility matrix for Android API 26–36.
