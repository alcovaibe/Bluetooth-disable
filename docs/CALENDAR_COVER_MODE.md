# Calendar Cover Mode — 1.0.8 (versionCode 9)

The starting `main` already contained versionName 1.0.7 and versionCode 8.
This change increments versionCode exactly once and adds only the Calendar cover.
Default and Calculator remain supported. Notes/Gallery enum values and old aliases
remain reserved, but they are not selectable or implemented as cover modes.

## User flow

1. Main screen → Change icon → Calendar → Continue.
2. Choose an access date and a note text (3–100 Unicode characters after trimming).
3. Review the displayed date/text and instructions → Finish setup.
4. The old task is cleared and the launcher opens the local Calendar.
5. Tap the month/year heading to choose a distant date. The Material date picker
   provides year selection and a text-input toggle; supported years are 1–9999.
6. On the chosen date, add a note containing the configured text and save it.
   Saving never opens the real app. Tap the saved note to enter MainActivity.
7. Hide opens a fresh Calendar at today and clears MainActivity from the task.
   Back then exits Calendar instead of revealing MainActivity.

Cancel, Back or process death before Finish setup does not write configuration or
change the launcher. No access note is automatically created by setup.

## Components

- `CalendarCoverSetupActivity`: independent setup and final confirmation, in-memory
  draft secrets, asynchronous activation. Both activities use existing app language
  and theme preferences. Application strings have English and Russian resources.
- `CalendarCoverActivity` / `CalendarScreen` / `CalendarViewModel`: Monday-first
  month grid, selection/today markers, adjacent months, direct date selection,
  scrollable notes, add/edit/delete and ordinary storage-error retry handling.
- `CalendarDates`: `LocalDate` / `YearMonth` calculations, localized display and UTC
  conversion for the Material picker. Stored dates use ISO `YYYY-MM-DD`.
- `CalendarNote`: UUID, LocalDate, text and creation/update timestamps. A date can
  have multiple notes. Editing preserves the note ID and creation timestamp.
- `CalendarNotesRepository` / `LocalCalendarNotesRepository`: local JSON model,
  synchronized repository access and `AtomicFile` replacement. UI never touches
  SharedPreferences or files. Disk and verification work run on `Dispatchers.IO`.
- `CalendarNotesCipher`: the JSON payload is encrypted with AES-256-GCM and a
  non-exportable Android Keystore key. The on-disk envelope holds version/IV/data;
  even a saved access note is not plaintext in files. Corrupt or undecryptable data
  raises a normal storage error rather than silently replacing existing notes.
- `CalendarAccessManager`: stores the ISO secret date plus a Base64 HMAC verifier,
  never a configured text. The HMAC-SHA256 key is device-local Android Keystore.
  The signed payload includes a domain prefix, date and trimmed text. Verification
  requires the exact date and case-sensitive text; only leading/trailing whitespace
  is normalized. Verifier comparison uses `MessageDigest.isEqual`.

All cards look the same. On a card tap, `verify(note.date, note.text)` either calls
`CoverModeNavigator.openMainFromCover(CALENDAR)` or opens the ordinary editor.
There is no password/date error, special lock icon, color or secret-link label.
Every card has ordinary Edit/Delete controls, including a matching note.

## Activation and navigation

`CoverModeManager` coordinates Default/Calculator/Calendar activation. It writes a
durable rollback journal containing the previous mode, launcher/hidden state and
opaque configuration values before changing configuration. Previous Keystore keys
remain available until the new configuration, alias and mode are persisted and
the journal is cleared (commit point). Failure/process death before that point
restores the previous state, including when reconfiguring the same cover mode.
Old pre-journal pending setup is reset to Default for upgrade safety.

After successful activation, the previous mode's access verifier/key is removed.
Startup also clears inactive access configurations in case the process died after
the commit point but before cleanup. Switching either way always uses that mode's
setup. Calendar notes remain local across ordinary mode switches. An explicit
reset or detected device transfer clears notes and their encryption key too.

The existing navigator centrally maps Calculator and Calendar to their activities.
It passes an internal origin to non-exported MainActivity and checks the active mode
before accepting it. Calendar and setup activities are non-exported; only the
Calendar launcher alias is exported. Hide/initial cover launch use
`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` with the cover activity.
The alias icon reuses the existing `calendar_icon.webp`; its localized name is
Calendar / Календарь. No Internet/calendar permissions, CalendarContract, accounts,
cloud integrations or analytics are introduced.

Existing all-data backup/D2D exclusion rules remain in force. DeviceTransferGuard
explicitly clears the Calendar verifier, note encryption key, files and preferences,
and restores Default launcher state. Copied verifiers/files cannot be used without
the original device's Keystore keys.

## Validation

Unit coverage includes exact/trimmed/case-sensitive date/text rules, Unicode length,
all month lengths and Monday-first alignment, leap/century years, invalid leap dates,
December/January boundaries, ISO storage, UTC picker conversions and RU/EN display.
Existing Calculator and Bluetooth protection unit tests remain in the suite.

Android instrumentation covers the real Keystore verifier, copied verifier rejection,
encrypted note persistence/edit/delete/date assignment and key-loss rejection,
launcher alias targets, final setup confirmation/cancellation, save-without-unlock,
ordinary mismatches, matching note → Main → Hide → Back, both mode transitions,
reset and interrupted activation recovery including same-mode reconfiguration.

Android CI retains its build/unit/lint/test-APK job and adds emulator instrumentation
on API 29 and 36 with uploaded test reports. Run locally with:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

Manual UI checks: RU/EN, dark/light themes, year/date picker input, February 2024,
editing/deleting a matching access note with its ordinary controls, canceled setup
from Calculator, Hide followed by Back and fresh launcher launch at today.

## Deliberate scope limits

No Notes Cover Mode, Gallery Cover Mode, system calendar import, reminders,
notifications, online sync or telemetry. No automatic merge, release tagging or
signed production APK publishing is performed by this PR.
