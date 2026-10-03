# Calculator Cover Mode

## Purpose

Calculator Cover Mode replaces the Bluetooth Disable launcher identity and opens a functional calculator instead of the main application screen. The disguise does not change the underlying Bluetooth protection functionality.

## Setup and access

1. The user selects the Calculator launcher identity.
2. After confirmation, a five-digit access code is configured.
3. After activation, the application restarts under the Calculator launcher identity.
4. To open the main Bluetooth Disable interface, enter the configured five-digit code and press `=`.
5. The **HIDE** action in the main interface returns to the active Calculator Cover Mode.

The code accepts ASCII digits `0–9` only. The plaintext code is never persisted. Verification uses HMAC-SHA256 with a non-exportable key stored in Android Keystore.

## Calculator features

The current implementation supports:

- addition, subtraction, multiplication and division;
- parentheses and standard operator precedence;
- negative and decimal numbers;
- percentages;
- division-by-zero and invalid-expression handling;
- history for up to 50 recent calculations;
- locale-aware decimal separators;
- light and dark themes.

Expressions are evaluated by the application's own parser/evaluator using `BigDecimal`.

## Emergency recovery

If the access code is forgotten, two recovery paths are available:

- open the application from the Quick Settings tile when the tile is installed;
- open calculator history and hold the **History** header for **3 seconds**.

The hold starts Android system authentication. After successful authentication, the user must confirm the cover reset. Cancelling or failing authentication leaves the active cover unchanged.

## Storage and device transfer

The access verifier and history are local-only. Application data is excluded from Android backup and device-to-device transfer. `DeviceTransferGuard` adds a second protection layer based on a non-exportable Keystore key and resets migrated local state to clean-install state when copied app data is detected on another device.

## Main components

- `CalculatorCoverActivity` — cover container and recovery flow;
- `CalculatorViewModel` — calculator state;
- `CalculatorEngine`, `CalculatorTokenizer`, `CalculatorParser`, `CalculatorEvaluator` — calculation layer;
- `CalculatorAccessCodeManager` — protected code verification;
- `CalculatorHistoryStore` — local history;
- `CalculatorHistoryDrawer` — history UI and recovery target.

## Verification

The mode is covered by unit and instrumentation tests for:

- five-digit access policy;
- calculator evaluation;
- access verifier persistence and validation;
- cover/main navigation;
- recovery and authentication cancellation;
- recovery hold gesture.

Instrumentation tests run in the shared CI compatibility matrix for Android API 26–36.
