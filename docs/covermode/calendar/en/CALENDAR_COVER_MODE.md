# Calendar Cover Mode — 1.0.19 (versionCode 21)

Calendar Cover Mode disguises Bluetooth Disable as a local calendar. The Calendar data model,
secret access verifier and recovery flow remain local to the device and do not require calendar,
account, Internet or cloud permissions.

## Hidden access

1. Main screen → Change icon → Calendar → Continue.
2. Choose an access date and secret note text.
3. Confirm setup. Setup stores the access rule but does not create the matching note.
4. In Calendar Cover Mode, manually create a note on the configured date with the configured text.
5. Tapping an exact matching note opens the real app immediately.

The access rule remains date-bound and case-sensitive. Leading/trailing whitespace is normalized.
The verifier is HMAC-SHA256 with a non-exportable Android Keystore key; the configured secret text
is not stored as plaintext. Access text remains limited to 3–100 UTF-16 code units so every valid
secret can also be represented by a Calendar note.

Changing the text of the matching note makes it an ordinary note until it once again exactly matches
the configured date/text rule. Setup never creates an access note automatically, and a successful
unlock never deletes the matching note.

## Calendar notes

- Notes are plain text only, with internal line breaks allowed.
- Leading/trailing whitespace and line breaks are trimmed on save.
- Empty notes are rejected.
- Maximum saved note length: 120 UTF-16 code units (`String.length`).
- Maximum notes per date: 25.
- Exact duplicates on the same date are rejected after edge-whitespace normalization; case still matters.
- Existing note dates cannot be changed by editing.
- Notes are ordered by most recent `updatedAt`; a newly saved note is therefore shown first, and editing
  moves that note to the top.
- Tapping an ordinary non-matching note opens a read-only viewer. Edit/Delete remain separate card actions.
- Delete always requires confirmation.

`LocalCalendarNotesRepository` stores notes in an `AtomicFile`. The JSON payload is encrypted with
AES-256-GCM using a non-exportable Android Keystore key. Corrupt or undecryptable data is surfaced as
a storage error and is never silently replaced.

## Persistent drafts

Unsaved editor text is persisted as an encrypted draft and survives activity/app restarts. New-note
drafts are bound to their original date; edit drafts are bound to the note ID. Drafts are removed after
a successful save, or when the user clears the text. Calendar drafts use the same device-local encrypted
storage boundary as Calendar notes and are cleared with Calendar local data.

## Calendar navigation and dates

- Every fresh Calendar Cover launch starts on today.
- Returning through Hide starts on today.
- Today selects the current local date and month.
- Previous/Next month always selects day 1 of the destination month.
- Supported year range: 1–9999.
- Material Date Picker conversion is performed through UTC calendar-date milliseconds, so changing the
  device timezone must not shift a selected date by ±1 day.
- RU/EN localization changes UI/date rendering only; user note text is never translated or rewritten.

## Emergency recovery

The shared hidden recovery gesture remains a passive three-second hold. Calendar exposes it on both the
Calendar title and Today action. A successful hold starts Android system authentication, then a separate
reset confirmation. There is no recovery haptic feedback or visible hold progress.

Confirmed Calendar recovery disables the disguise and restores the default launcher while preserving
all Calendar notes/drafts. Authentication cancellation/failure, confirmation cancellation, lifecycle
changes and repeated attempts must not reset the mode accidentally.

## Backup, transfer and reinstall

All application-private files/preferences remain excluded from Android cloud backup and device-to-device
transfer. `DeviceTransferGuard` is the additional OEM-migration defense. Calendar notes, drafts and secret
configuration are not intended to move to another device. Uninstall/reinstall starts Calendar data from
an empty state.

## Audit validation

The audit regression suite covers:

- access date/text exactness, normalization, case sensitivity and Keystore-backed verification;
- 120-unit text limit, blank/duplicate rejection and the 25-notes-per-date limit;
- creation/update ordering and immutable note dates;
- encrypted note and draft persistence plus key-loss/corruption behavior;
- read-only ordinary-note taps and unchanged explicit Edit/Delete actions;
- draft persistence across Calendar activity restarts;
- first-day month navigation, leap/century years, 1–9999 boundaries and timezone-safe picker conversion;
- matching note → Main → Hide → fresh today state;
- shared three-second authenticated emergency recovery and preservation of Calendar user data.

CI is expected to run unit tests, lint, debug Android tests and release assembly, with instrumented coverage
across API 26–36 where the GitHub emulator environment supports that level. Physical-device recovery and
OEM-specific UI behavior remain manual release checks.
