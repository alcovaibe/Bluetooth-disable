# Calculator Cover Mode

## Purpose

**Calculator Cover Mode** is a disguise mode that presents Bluetooth Disable as a regular calculator application.

After the mode is activated:

- a calculator is shown in the launcher instead of the standard Bluetooth Disable entry;
- a separate name and icon are used;
- launching the launcher icon opens a fully functional calculator interface;
- the main Bluetooth Disable interface is not shown directly to the user;
- a preconfigured five-digit code is used to enter the main interface;
- the code is entered as a normal number and confirmed with the `=` button;
- the mode has its own calculation history;
- emergency access recovery is available if the code is forgotten;
- emergency recovery requires Android system authentication;
- calculation history is preserved when the disguise is reset.

Calculator Cover Mode is not a static screen or a calculator imitation. It is a standalone local calculator with its own expression parser, evaluator, history, and user interface.

The main implementation of the mode is located in:

`app/src/main/java/com/pulse/bluetoothdisable/cover/calculator/`

---

# 1. General architecture

The mode consists of several separate layers:

```text
Launcher alias
      │
      ▼
CalculatorCoverActivity
      │
      ▼
CalculatorScreen
      │
      ▼
CalculatorViewModel
      │
      ├──────────────► CalculatorAccessCodeManager
      │
      ├──────────────► CalculatorHistoryStore
      │
      ▼
CalculatorEngine
      │
      ▼
CalculatorTokenizer
      │
      ▼
CalculatorParser
      │
      ▼
Calculator AST
      │
      ▼
CalculatorEvaluator
      │
      ▼
CalculatorFormatter
```

The following components operate separately from the calculation layer:

```text
CoverModeManager
LauncherIconController
CoverModeStore
CoverModeNavigator
CoverRecoveryManager
CoverDeviceAuthenticator
CoverRecoveryGesture
CoverSystemUi
```

This separation keeps calculation logic independent from the disguise mechanism, launcher handling, access-code storage, and emergency recovery.

---

# 2. User flow

## 2.1. Activation

The user opens the main Bluetooth Disable interface and selects:

```text
Change icon
        ↓
Calculator
        ↓
Confirmation
        ↓
Create a five-digit code
        ↓
Repeat the code
        ↓
Activate Calculator Cover Mode
```

`CalculatorCoverConfirmationDialog` is shown first.

After confirmation, `CalculatorCoverSetupDialog` opens.

The user enters two values:

- the access code;
- the repeated code.

The code must consist of exactly five ASCII digits:

```text
00000
01234
58317
99999
```

> **Note:** the code `58317` is used throughout this documentation only as an example access code. The application does not have a preset `58317` code; the actual code is chosen by the user during mode setup.

Leading zeros are allowed.

The following are not allowed:

```text
1234
123456
12a45
١٢٣٤٥
```

After successful validation, the following is called:

```kotlin
CoverModeManager.activateCalculator(code)
```

The manager:

1. validates the code;
2. stores a protected verifier for the code;
3. switches the launcher to Calculator;
4. sets the active mode to `CALCULATOR`;
5. completes the mode-switch transaction;
6. removes the access configuration of the previous Cover Mode;
7. opens `CalculatorCoverActivity`.

---

# 3. Launcher and startup

A separate launcher alias is used for Calculator:

```text
LauncherAliasCalculator
```

It routes startup to:

```text
CalculatorCoverActivity
```

`CalculatorCoverActivity` itself has:

```text
android:exported="false"
android:launchMode="singleTask"
```

External applications should not launch the calculator Activity directly.

Only the launcher alias is exported because Android Launcher must be able to launch the corresponding component.

After activation, `LauncherIconController` keeps only the alias for the selected mode enabled.

When switching, the new alias is enabled first and the remaining aliases are disabled afterward. This prevents the application from temporarily having no launcher component during the switch.

On Android 12 and newer, `CalculatorCoverActivity` uses a separate theme:

```text
Theme.BluetoothDisable.Cover.Calculator
```

That theme assigns the calculator icon to the system Splash Screen.

---

# 4. Entering the main Bluetooth Disable interface

The primary entry method is hidden inside a normal calculator operation.

The user enters a code, for example:

```text
58317
```

and presses:

```text
=
```

`CalculatorViewModel.equalsPressed()` first checks whether the current expression is a valid five-digit code candidate.

If the expression has the form:

```text
NNNNN
```

it is passed to:

```kotlin
CalculatorAccessCodeManager.verify()
```

If the verifier matches:

```text
CalculatorViewModel
        ↓
equalsPressed() = true
        ↓
CalculatorScreen
        ↓
onUnlock()
        ↓
CoverModeNavigator.openMainFromCover()
        ↓
MainActivity
```

Before navigation, the current calculator expression state is cleared.

`MainActivity` receives an internal marker indicating that it was opened from a specific Cover Mode.

Only in this case does the main interface show the **“HIDE”** action.

---

# 5. Returning to Calculator

When **“HIDE”** is pressed, the main interface calls:

```kotlin
CoverModeNavigator.hideToCoverMode(...)
```

For Calculator, the destination is:

```text
CalculatorCoverActivity
```

The Activity is started with:

```text
FLAG_ACTIVITY_NEW_TASK
FLAG_ACTIVITY_CLEAR_TASK
```

As a result, the main interface is removed from the current task.

This is important for the disguise flow:

```text
Calculator
    ↓
secret code
    ↓
Bluetooth Disable
    ↓
HIDE
    ↓
Calculator
```

After returning, pressing Back must not unexpectedly reveal Bluetooth Disable.

---

# 6. Calculation pipeline

Normal calculations pass through the following stages:

```text
User input
        ↓
CalculatorViewModel
        ↓
expression string
        ↓
CalculatorEngine
        ↓
CalculatorTokenizer
        ↓
token list
        ↓
CalculatorParser
        ↓
AST
        ↓
CalculatorEvaluator
        ↓
BigDecimal
        ↓
CalculatorFormatter
        ↓
result string
        ↓
CalculatorHistoryStore
```

This separation keeps UI concerns independent from syntax analysis and calculation.

---

# 7. Supported mathematical operations

The calculator supports:

- addition;
- subtraction;
- multiplication;
- division;
- unary minus;
- parentheses;
- decimal numbers;
- percentages.

The following symbols are supported:

```text
+
-
−
×
*
÷
/
%
(
)
.
,
```

Both dot and comma are recognized as decimal separators.

The internal representation of decimal numbers uses a dot.

---

# 8. Operator precedence

The parser implements the standard order:

```text
1. Parentheses / numbers
2. Unary minus
3. Percentage
4. Multiplication / division
5. Addition / subtraction
```

For example:

```text
2 + 3 × 4
```

is evaluated as:

```text
2 + 12 = 14
```

while:

```text
(2 + 3) × 4
```

is evaluated as:

```text
5 × 4 = 20
```

---

# 9. Percentage semantics

Percentage is a postfix operation.

On its own:

```text
50%
```

is equal to:

```text
0.5
```

For multiplication:

```text
200 × 10%
```

produces:

```text
20
```

For division:

```text
200 ÷ 10%
```

produces:

```text
2000
```

For addition and subtraction, standard calculator percentage semantics relative to the left operand are used.

For example:

```text
200 + 10%
```

is interpreted as:

```text
200 + 200 × 0.10
```

and produces:

```text
220
```

While:

```text
200 - 10%
```

produces:

```text
180
```

---

# 10. Calculation precision

The numeric type used is:

```kotlin
BigDecimal
```

This means operations with finite decimal representations avoid the common precision problems of `Double`.

For example:

```text
0.1 + 0.2
```

returns:

```text
0.3
```

Large integer values are also calculated exactly.

If a normal `BigDecimal.divide()` cannot be completed without an infinite decimal representation, the following is used:

```kotlin
MathContext.DECIMAL128
```

Division by zero is handled separately and converted into a controlled UI error.

---

# 11. Calculator Cover Mode files

## 11.1. `CalculatorAccessCodePolicy.kt`

Defines the rules for a valid access code.

### `CODE_LENGTH`

```kotlin
const val CODE_LENGTH = 5
```

The single code length used throughout the mode.

### `isValid(code)`

Checks that:

- the length is exactly five;
- every character is between `'0'` and `'9'`.

Only ASCII digits are accepted.

### `matches(code, confirmation)`

Checks both:

1. that the main code is valid;
2. that the code and confirmation match exactly.

Used during Calculator Cover Mode setup.

---

# 11.2. `CalculatorAccessCodeManager.kt`

Responsible for secure creation, storage, verification, and deletion of the access code.

The code itself is never stored in `SharedPreferences`.

Instead, the following is stored:

```text
HMAC-SHA256(code)
```

The HMAC is calculated using a key from:

```text
AndroidKeyStore
```

Only the Base64 representation of the HMAC result is stored in preferences.

## `setCode(code)`

1. validates the code through `CalculatorAccessCodePolicy`;
2. obtains an existing key or creates a new one;
3. calculates HMAC-SHA256;
4. encodes the result in Base64;
5. synchronously stores the verifier in SharedPreferences.

`commit()` is used so Cover Mode activation knows that the value was actually persisted before moving to the next transaction stage.

## `verify(code)`

Verifies the code entered by the user.

Sequence:

1. validate the code format;
2. obtain the stored verifier;
3. decode Base64;
4. obtain the existing AndroidKeyStore key;
5. calculate HMAC for the entered value;
6. compare both arrays using:

```kotlin
MessageDigest.isEqual()
```

If the verifier is corrupted, the key is missing, or a Keystore error occurs, `false` is returned.

Exceptions are not propagated to the caller.

## `hasCode()`

Checks that both conditions are true:

- the verifier exists;
- the corresponding key exists in AndroidKeyStore.

Used by `CoverModeManager.isCalculatorReady()`.

## `clearVerifier()`

Deletes the stored verifier from SharedPreferences but **does not delete the Keystore key**.

This separation exists specifically to support a safe Cover Mode reset transaction.

While the transition has not yet been committed, the old key remains available so that the previous verifier can be restored from the rollback snapshot if an error occurs.

## `clearCode()`

Completely removes the access configuration:

```text
verifier + Keystore key
```

## `deleteKey()`

Deletes the key from AndroidKeyStore.

Deletion errors are suppressed because the verifier has already been removed at this point and internal Keystore details should not be exposed to the user.

## `sign(key, code)`

Internal method that calculates:

```text
HMAC-SHA256
```

for the UTF-8 representation of the code.

## `getOrCreateKey()`

Returns the existing key or creates a new one.

## `existingKey()`

Reads the SecretKey from `AndroidKeyStore`.

## `loadKeyStore()`

Opens Android Keystore and loads its state.

## `generateKey()`

Creates a new HMAC-SHA256 key with the purposes:

```text
PURPOSE_SIGN
PURPOSE_VERIFY
```

The key remains inside Android Keystore.

---

# 11.3. `CalculatorCoverDialogs.kt`

Contains the initial setup UI for the mode.

## `CalculatorCoverConfirmationDialog()`

A wrapper around the common:

```kotlin
CoverModeConfirmationDialog
```

Shows a warning before Calculator mode is enabled.

After confirmation, MainActivity proceeds to code setup.

## `CalculatorCoverSetupDialog()`

The main setup window.

Stores the following in Compose state:

```text
code
confirmation
setupFailed
```

The following values are calculated in real time:

```text
codeValid
confirmationValid
matches
```

The continue button is enabled only when:

- both values are valid;
- both values match exactly.

On confirmation, the following is called:

```kotlin
onComplete(code)
```

If Cover Mode activation fails, a setup error is shown.

## `AccessCodeField()`

Shared code input field.

Uses:

```text
PasswordVisualTransformation
KeyboardType.NumberPassword
```

Supports displaying errors for:

- invalid length;
- confirmation mismatch.

## `sanitizeCode()`

Filters user input.

Keeps only:

```text
0–9
```

and at most five characters.

---

# 11.4. `CalculatorCoverActivity.kt`

The main Android Activity for Calculator mode.

It connects:

- calculator UI;
- ViewModel;
- system theme;
- navigation;
- emergency recovery.

## `attachBaseContext()`

Passes the Context through:

```kotlin
LanguageManager.wrapContext()
```

This allows the mode to use the language selected in Bluetooth Disable.

## `onCreate()`

The main initialization entry point.

The following is checked first:

```kotlin
CoverModeManager(this).isCalculatorReady()
```

Calculator is considered ready only if all of the following are true:

```text
activeMode == CALCULATOR
launcherStyle == CALCULATOR
accessCode exists
```

If the state is corrupted or incomplete, Calculator Cover Activity does not continue normal startup and returns the user to `MainActivity`.

Then:

1. edge-to-edge is enabled;
2. `CoverModeManager` is created;
3. `CoverRecoveryManager` is created;
4. `CoverDeviceAuthenticator` is created;
5. `CalculatorViewModel` is obtained;
6. the Compose UI is started;
7. `CoverTheme` is applied;
8. `CalculatorScreen` is displayed.

`onUnlock` calls:

```kotlin
CoverModeNavigator.openMainFromCover(
    this,
    CoverMode.CALCULATOR
)
```

## `beginRecovery()`

Starts emergency disguise reset.

The method:

1. checks that the Activity is in the `RESUMED` state;
2. asks `CoverRecoveryManager` to begin a new attempt;
3. performs haptic feedback;
4. starts system authentication;
5. passes the result back to Recovery Manager.

If system authentication is unavailable, the attempt is cancelled and a Toast is shown.

## `resetCover()`

Called after:

```text
7-second hold
→ successful system authentication
→ user confirmation
```

`CoverRecoveryManager.confirmReset()` calls:

```kotlin
CoverModeManager.resetCalculatorCover()
```

After a successful reset, the normal Bluetooth Disable interface is opened through:

```kotlin
CoverModeNavigator.openDefaultMain()
```

## `onResume()`

Sets:

```text
resumed = true
```

This enables the hidden recovery gesture.

## `onPause()`

Disables the recovery gesture.

If the user leaves the Activity while the confirmation dialog is active, the reset operation is cancelled.

## `onDestroy()`

Cancels unfinished recovery state and closes `CoverDeviceAuthenticator`.

## `onNewIntent()`

Because the Activity uses `singleTask`, a new launcher start may be delivered through `onNewIntent()`.

In that case:

- the previous recovery flow is cancelled;
- the current calculator expression is cleared.

---

# 11.5. `CalculatorScreen.kt`

The main Compose UI of the calculator.

Receives:

```kotlin
CalculatorViewModel
onUnlock
recoveryEnabled
onRecoveryHold
```

## `CalculatorScreen()`

Builds the full main screen.

Contains:

- the history button;
- the history drawer;
- the previous calculation line;
- the current expression/result;
- error messages;
- the calculator keypad.

For Russian, the displayed decimal dot is replaced with a comma.

The internal mathematical expression remains normalized.

For long results, the text size is reduced automatically:

```text
up to 20 characters → 52sp
21–34               → 36sp
more than 34        → 28sp
```

The screen supports horizontal scrolling for long expressions.

Button handling:

```text
C  → clear()
⌫  → backspace()
%  → inputPercent()
÷  → inputOperator('÷')
×  → inputOperator('×')
−  → inputOperator('-')
+  → inputOperator('+')
() → inputParenthesis()
,  → inputDecimal()
=  → equalsPressed()
0–9 → inputDigit()
```

If `equalsPressed()` returns `true`, `onUnlock()` is called.

## `CalculatorKeypad()`

Builds an adaptive keypad.

Button size is calculated from the available width and constrained to:

```text
56dp–92dp
```

The keypad is split into:

- a block of normal keys;
- a vertical operator rail.

## `CalculatorThreeKeyRow()`

Creates one row of three round buttons.

## `CalculatorCircleKey()`

Draws a normal circular button.

The `C` button is visually highlighted using:

```text
errorContainer
```

The remaining buttons use:

```text
surfaceVariant
```

## `CalculatorOperatorRail()`

Creates the vertical panel:

```text
÷
×
−
+
=
```

The `=` button is separately highlighted using the primary Material Theme color.

---

# 11.6. `CalculatorViewModel.kt`

The central user-interaction logic.

The ViewModel does not directly handle Compose layout.

It manages:

- the current expression;
- the result;
- errors;
- history;
- post-calculation state;
- secret-code verification.

## `CalculatorUiState`

Contains:

### `expression`

The current mathematical expression.

### `previousExpression`

The previous calculation line, for example:

```text
2+2 =
```

### `display`

The value shown in the large display text.

### `error`

One of the Calculator Engine errors or `null`.

### `afterResult`

Indicates that the previous action completed with `=`.

Used to control the behavior of the next input.

### `history`

The list of stored calculations.

---

## `inputDigit(digit)`

Adds a digit.

If a result was previously produced, the old expression is replaced by the new input.

---

## `inputDecimal()`

Adds a decimal separator.

The method:

- prevents two decimal points in the same number;
- automatically creates `0.` after an operator;
- creates `0.` in an empty expression;
- also creates `0.` after `(`.

---

## `inputOperator(operator)`

Handles:

```text
+
-
×
÷
```

Several states are taken into account.

After a result:

```text
2 + 2 = 4
```

pressing `+` creates:

```text
4+
```

At the beginning of an expression, only `-` is allowed because it may represent unary minus.

The method also:

- replaces a previous binary operator with the new one;
- allows unary minus after an operator;
- completes a number ending in a decimal point;
- allows an operator after a number, `%`, or `)`.

---

## `inputPercent()`

Adds `%` if the expression ends with:

- a digit;
- a closing parenthesis.

After a completed result, `%` is not added immediately.

---

## `inputParenthesis()`

A single button controls both parentheses.

An opening `(` is added:

- to an empty expression;
- after another `(`;
- after a binary operator.

A closing `)` is added only if:

- there are more opening parentheses than closing parentheses;
- the preceding character is a number, `%`, or `)`.

---

## `backspace()`

Removes the last character of the expression.

It also:

- clears the error;
- clears the `afterResult` state;
- returns the display to `0` if the expression becomes empty.

---

## `clear()`

Completely clears the current calculation state but **does not delete history**.

---

## `clearHistory()`

Deletes history from `CalculatorHistoryStore` and updates UI state.

---

## `equalsPressed()`

The key method of the mode.

It performs two functions at the same time:

1. normal calculation;
2. hidden access-code verification.

First:

```kotlin
CalculatorAccessCodePolicy.isValid(raw)
```

determines whether the entered value looks like a five-digit code.

If the format matches and:

```kotlin
CalculatorAccessCodeManager.verify(raw)
```

returns `true`, the ViewModel:

- clears the current screen;
- returns `true`.

`CalculatorScreen` interprets this as a command to open the main Bluetooth Disable interface.

If the code does not match, the expression is processed as a normal number or mathematical expression.

However, **any expression consisting of exactly five digits is not written to history**, even when it is not the correct code.

This prevents entered five-digit access candidates from appearing in calculator history.

For a normal expression:

1. `CalculatorEngine.evaluate()` is run;
2. the result is formatted;
3. the expression is stored in history;
4. the UI enters the `afterResult` state.

If an error occurs, the corresponding `CalculatorEngineError` is set.

Return value:

```text
true  → code accepted; Bluetooth Disable should be opened
false → normal calculation or error
```

---

## `updateExpression()`

Internal helper method.

Synchronously updates:

- expression;
- previousExpression;
- display;
- error;
- afterResult.

---

## `isBinaryOperator()`

Checks whether a character is one of:

```text
+ - × ÷
```

---

# 11.7. `CalculatorHistoryStore.kt`

Responsible for local history storage.

## `CalculatorHistoryEntry`

Contains:

```text
expression
result
```

For example:

```text
expression = "200+10%"
result = "220"
```

## `entries()`

Reads history from SharedPreferences.

The number of entries is forcibly constrained to:

```text
0..50
```

A corrupted incomplete entry with a missing expression or result is skipped.

## `add(expression, result)`

Adds a new entry to the beginning of the list.

History is then truncated to:

```text
MAX_ENTRIES = 50
```

SharedPreferences is completely rewritten with the current list.

A synchronous commit is used.

## `clear()`

Completely clears history.

## `expressionKey(index)`

Creates keys such as:

```text
expression_0
expression_1
...
```

## `resultKey(index)`

Creates:

```text
result_0
result_1
...
```

History is a separate user-data store and is not deleted during a normal emergency reset of Calculator Cover Mode.

---

# 11.8. `CalculatorHistoryDrawer.kt`

Compose UI for calculation history.

## `CalculatorHistoryDrawer()`

Creates a side panel with a width of:

```text
290dp
```

The panel contains:

1. the “History” header;
2. the list of calculations;
3. the clear button.

If history is empty, an empty-history message is shown.

`LazyColumn` is used for the list.

### Hidden recovery target

The drawer header is the hidden emergency-recovery area:

```kotlin
.coverRecoveryHold(...)
```

Recovery is enabled only if:

- recovery is allowed by the Activity;
- the drawer is open;
- the drawer is no longer animating.

Holding the header for 7 seconds starts the recovery flow.

History rows and the clear button are not recovery targets.

## `CalculatorHistoryRow()`

Builds a line in the form:

```text
expression = result
```

For the Russian interface, the decimal dot is replaced with a comma.

Short content uses one line.

If the configured length threshold is exceeded, two lines are allowed.

Overflow is displayed with an ellipsis.

---

# 11.9. `CalculatorEngine.kt`

The public entry point of the calculator calculation engine.

## `CalculatorEvaluation`

Represents the engine result.

### `Success`

Contains:

```kotlin
BigDecimal
```

### `Failure`

Contains:

```kotlin
CalculatorEngineError
```

## `CalculatorEngineError`

Currently:

```text
DIVISION_BY_ZERO
INVALID_EXPRESSION
```

## `evaluate(expression)`

Runs the complete pipeline:

```text
String
 ↓
Tokenizer
 ↓
Parser
 ↓
AST
 ↓
Evaluator
 ↓
BigDecimal
```

An empty expression is immediately considered invalid.

Parser and arithmetic errors are not exposed as exceptions to callers.

They are converted into a controlled:

```text
CalculatorEvaluation.Failure
```

---

# 11.10. `CalculatorTokenizer.kt`

Converts the source string into tokens.

## `CalculatorTokenType`

Defines:

```text
NUMBER
PLUS
MINUS
MULTIPLY
DIVIDE
PERCENT
LEFT_PAREN
RIGHT_PAREN
EOF
```

## `CalculatorToken`

Contains:

```text
type
text
```

The `text` field is primarily used for numbers.

## `CalculatorParseException`

The unified exception type for invalid syntax.

## `CalculatorTokenizer.tokenize()`

Processes the string character by character.

Supports:

### Numbers

```text
0–9
```

### Decimal separators

```text
.
,
```

Comma is normalized to a dot.

### Minus

```text
-
−
```

### Multiplication

```text
×
*
```

### Division

```text
÷
/
```

### Other symbols

```text
+
%
(
)
```

Whitespace is ignored.

Any unknown character causes `CalculatorParseException`.

Only one decimal separator is allowed in a single number.

After all tokens, the following is added automatically:

```text
EOF
```

---

# 11.11. `CalculatorAst.kt`

Defines the internal abstract syntax tree — AST.

## `CalculatorNode`

The common sealed interface for all nodes.

## `NumberNode`

Stores a numeric value:

```kotlin
BigDecimal
```

## `UnaryMinusNode`

Represents:

```text
-x
```

## `PercentNode`

Represents:

```text
x%
```

## `BinaryNode`

Represents a binary operation:

```text
left operator right
```

## `BinaryOperator`

Supports:

```text
ADD
SUBTRACT
MULTIPLY
DIVIDE
```

The AST separates expression syntax from actual evaluation.

---

# 11.12. `CalculatorParser.kt`

Converts a token list into an AST.

A recursive descent parser is used.

## `parse()`

Starts parsing at the addition/subtraction level.

After completion, `EOF` is required.

If unprocessed tokens remain, the expression is considered invalid.

## `parseAddSubtract()`

Parses:

```text
+
-
```

Its operands are results from `parseMultiplyDivide()`.

This gives addition and subtraction lower precedence.

## `parseMultiplyDivide()`

Parses:

```text
×
÷
```

Uses `parsePercent()`.

## `parsePercent()`

Handles postfix:

```text
%
```

Repeated application of `%` is technically supported.

## `parseUnary()`

Implements unary minus.

For example:

```text
-5
2 × -3
(-5)
```

## `parsePrimary()`

Handles:

- a number;
- an expression in parentheses.

An opening parenthesis requires a matching closing parenthesis.

NUMBER text is converted into `BigDecimal`.

## `match(type)`

Checks the current token.

If it matches, the current position advances by one element.

## `current()`

Returns the parser's current token.

---

# 11.13. `CalculatorEvaluator.kt`

Evaluates the completed AST.

## `CalculatorDivisionByZeroException`

A dedicated internal exception for division by zero.

It allows `CalculatorEngine` to distinguish this case from a generic invalid expression.

## `CalculatorEvaluator.evaluate(node)`

Recursively evaluates any node type.

### NumberNode

Returns the number.

### UnaryMinusNode

Evaluates the operand and negates it.

### PercentNode

Divides the operand by:

```text
100
```

### BinaryNode

Delegates to `evaluateBinary()`.

## `evaluateBinary()`

Handles binary operations.

A special case exists for:

```text
A + B%
A - B%
```

In this case:

```text
delta = A × B%
```

is calculated, followed by:

```text
A + delta
```

or:

```text
A - delta
```

For all other operations, the right operand is evaluated normally.

## `divide()`

First checks for division by zero.

Then it attempts an exact:

```kotlin
left.divide(right)
```

If the decimal representation is non-terminating, the following is used:

```kotlin
MathContext.DECIMAL128
```

---

# 11.14. `CalculatorFormatter.kt`

Formats mathematical results for the UI and history.

## `format(value)`

If the value is mathematically equal to zero:

```text
0
```

the exact string `"0"` is returned.

Then:

```kotlin
stripTrailingZeros()
```

is called.

Therefore:

```text
2.0000
```

is displayed as:

```text
2
```

Plain decimal representation is used while its length does not exceed:

```text
60 characters
```

For very long values:

```kotlin
toEngineeringString()
```

is used.

## `localize(value, useComma)`

For locales where the calculator interface uses a comma, replaces:

```text
.
```

with:

```text
,
```

This affects display formatting only.

Internal expressions and calculations remain normalized.

---

# 12. Emergency recovery

If the user forgets the five-digit code, the mode provides an independent recovery path.

Flow:

```text
Open Calculator
        ↓
Open History
        ↓
Hold the History header for 7 seconds
        ↓
Haptic feedback
        ↓
Android system authentication
        ↓
Reset confirmation
        ↓
Calculator Cover Mode → DEFAULT
```

The hold by itself **does not reset the mode**.

It only starts system authentication.

---

# 13. `CoverRecoveryGesture.kt`

A shared Cover Mode component used by Calculator.

## `coverRecoveryHold()`

A Compose Modifier that tracks a continuous hold for:

```text
7000 ms
```

Properties:

- small finger movement is allowed;
- an unconsumed pointer event is not required;
- it works inside drawers and other Compose components;
- releasing the finger before 7 seconds cancels the timer;
- after one hold fires, the callback does not fire again until the next gesture.

This improves reliability of the hidden gesture across different OEM devices.

---

# 14. `CoverRecoveryManager.kt`

The shared emergency-recovery state machine.

For Calculator, the default is:

```text
modeToRecover = CALCULATOR
```

States:

```text
IDLE
AUTHENTICATING
CONFIRMING
RESETTING
```

## `begin()`

Starts an attempt only if:

- the current state is `IDLE`;
- the correct Cover Mode is active.

Returns a unique attempt ID.

## `authenticationSucceeded(id)`

Accepts successful authentication only if:

- the ID matches the current attempt;
- the state is still `AUTHENTICATING`;
- the active Cover Mode has not changed.

The state then becomes:

```text
CONFIRMING
```

## `authenticationRejected(id)`

Cancels the current attempt when authentication fails.

## `cancel()`

Increments the internal attempt ID and returns the state to `IDLE`.

Because the ID is incremented, a late callback from an old BiometricPrompt cannot authorize a new attempt.

## `confirmReset()`

Allows reset only after the state:

```text
CONFIRMING
```

and only if Calculator Cover Mode is still active.

It then calls:

```kotlin
resetCover()
```

For Calculator, this is:

```kotlin
CoverModeManager.resetCalculatorCover()
```

After completion, the manager always returns to `IDLE`.

---

# 15. `CoverDeviceAuthenticator.kt`

A shared wrapper around AndroidX `BiometricPrompt`.

It does not store:

- PIN;
- password;
- pattern;
- biometric data;
- successful authentication token.

All verification is performed by Android.

## Android API 26–29

Uses:

```text
BIOMETRIC_WEAK | DEVICE_CREDENTIAL
```

## Android API 30+

Uses:

```text
BIOMETRIC_STRONG | DEVICE_CREDENTIAL
```

This allows the user to verify identity using a compatible system method.

## `authenticate()`

Checks authentication availability through:

```kotlin
BiometricManager.canAuthenticate()
```

Then starts the system prompt.

Only one active attempt is allowed at a time.

## `close()`

Cancels the active prompt and prevents new attempts for the instance being destroyed.

---

# 16. Resetting Calculator Cover Mode

After successful system verification and additional confirmation, the following is called:

```kotlin
CoverModeManager.resetCalculatorCover()
```

The operation:

1. confirms that the current mode is actually Calculator;
2. begins a transaction to transition to `DEFAULT`;
3. removes the calculator verifier;
4. switches the launcher to the standard entry;
5. verifies that only the standard launcher alias is enabled;
6. commits `CoverMode.DEFAULT`;
7. clears the rollback journal;
8. deletes the old HMAC key only after the commit point.

**CalculatorHistoryStore is not cleared**.

Notes and Gallery user data are not deleted either.

---

# 17. Transactional Cover Mode switching

Launcher switching and secret configuration must not be treated as a set of unrelated independent operations.

For example, the Android process may be terminated between:

```text
saving the code
```

and:

```text
enabling the launcher alias
```

Therefore, `CoverModeManager` uses a rollback journal.

Before a transition begins, a snapshot is stored containing:

```text
previous active mode
previous launcher style
launcher hidden state
calculator access verifier
calendar access verifier
notes access verifier
gallery access verifier
```

Then `CoverModeStore` stores:

```text
pending mode
rollback snapshot
```

Only after that is the real configuration modified.

---

# 18. Commit point

For Calculator activation, the simplified sequence is:

```text
beginTransition()
      ↓
setCode()
      ↓
launcher.setStyle(CALCULATOR)
      ↓
setActiveMode(CALCULATOR)
      ↓
clearPending()
      ↓
COMMIT
      ↓
clearInactiveAccess()
```

`clearPending()` is the point after which the new configuration is considered durably committed.

If an exception occurs before this point, `CoverModeManager` restores the snapshot.

---

# 19. Recovery after process death

When the application starts:

```kotlin
BluetoothDisablerApplication
```

calls:

```kotlin
CoverModeManager.recoverInterruptedSetup()
```

If an unfinished transaction exists:

```text
pendingMode != null
```

the manager reads the rollback snapshot and restores the previous consistent state.

For Calculator, the following is also checked:

```kotlin
isCalculatorReady()
```

All of the following must exist at the same time:

```text
CoverMode.CALCULATOR
LauncherStyle.CALCULATOR
valid access verifier + Keystore key
```

If consistency is broken, the application is restored to Default.

---

# 20. Why the key is deleted after the verifier

Emergency reset uses a specific order:

```text
clearVerifier()
        ↓
switch launcher
        ↓
change active mode
        ↓
commit point
        ↓
deleteKey()
```

This behavior is intentional.

If the Keystore key were deleted immediately and a later transaction step failed, the old verifier could be restored from the snapshot but could no longer be verified.

Therefore, the old key is preserved until a successful commit point.

---

# 21. `CoverModeNavigator.kt`

Responsible for safe navigation between the real application and Cover Mode.

## `openMainFromCover()`

Before opening MainActivity, it:

- checks that the mode belongs to a Cover Mode;
- verifies that it is still the active mode.

An internal origin is placed in the Intent:

```text
INTERNAL_COVER_ORIGIN
```

## `coverOrigin()`

MainActivity uses this method to determine whether it was genuinely opened from the active Cover Mode.

Providing an arbitrary stale Intent value is not enough because the active mode is checked again.

## `hideToCoverMode()`

Returns the user to Calculator and clears the task.

## `openDefaultMain()`

Used after emergency reset.

The method requires:

```text
activeMode == DEFAULT
```

and opens a new clean MainActivity task.

---

# 22. `CoverSystemUi.kt`

Calculator uses the shared Cover Modes system UI.

## `enableCoverEdgeToEdge()`

Enables edge-to-edge and makes the following transparent:

- status bar;
- navigation bar;
- navigation bar divider.

On Android 10+, forced navigation-bar contrast is disabled.

## `CoverTheme()`

Obtains the user's Bluetooth Disable theme through `ThemeManager`.

Supported values:

```text
LIGHT
DARK
SYSTEM
```

Status/navigation bar icon appearance is updated according to the active light or dark theme.

As a result, Calculator Cover Mode uses the same system-bar behavior as the main interface.

---

# 23. History and secret code

Calculator history and the access code are independent subsystems.

```text
calculator_history_preferences
```

contains normal calculation history.

```text
calculator_access_code_preferences
```

contains only the HMAC verifier for the secret code.

The five-digit code itself is not written to history when entered through Calculator.

In addition, any standalone five-digit number is treated as a potential access candidate and is not stored in history even when the code is incorrect.

Example:

```text
entered: 58318
example configured code: 58317
```

Calculator may process `58318` as a normal number, but the history entry:

```text
58318 = 58318
```

is not added.

---

# 24. Errors

Calculator Engine distinguishes two user-facing error categories.

## `DIVISION_BY_ZERO`

For example:

```text
1 ÷ 0
```

## `INVALID_EXPRESSION`

For example:

```text
2 + × 3
(2 + 3
1.2.3
%
2 ÷
```

Parser/evaluator exceptions must not reach the UI directly.

The user receives a localized error through `CalculatorUiState.error`.

---

# 25. Protection against inconsistent state

`CalculatorCoverActivity` does not trust the fact that it was launched by itself.

Before displaying the calculator, the following are checked:

```text
active mode
+
launcher style
+
verifier and Keystore key presence
```

If any element is missing, the Activity returns to MainActivity.

This prevents operation of a partially configured mode after:

- a failure;
- corrupted preferences;
- an interrupted transition;
- old configuration;
- loss of the Keystore key.

---

# 26. Recovery lifecycle safety

The recovery flow intentionally does not survive Activity recreation as an already authorized operation.

Reset authorization exists only in memory.

A new `CoverRecoveryManager` instance begins in:

```text
IDLE
```

Even if the previous instance already passed authentication successfully, the new instance requires a new system verification.

On:

```text
onPause
onDestroy
onNewIntent
```

unfinished operations are cancelled.

---

# 27. What is preserved during emergency reset

After successful Calculator recovery:

Removed:

```text
Calculator Cover Mode
Calculator access verifier
Calculator access Keystore key
Calculator launcher alias as the active alias
```

Restored:

```text
CoverMode.DEFAULT
standard launcher icon
standard application name
```

Preserved:

```text
Calculator history
Calendar notes
Notes user data
Gallery user data
other application user data
```

---

# 28. Testing

## `CalculatorEngineTest.kt`

Checks:

- addition;
- subtraction;
- multiplication;
- division;
- operator precedence;
- parentheses;
- unary minus;
- negative numbers after multiplication;
- decimal precision;
- percentage operations;
- division by zero;
- large integers;
- malformed expressions.

Examples of verified behavior:

```text
2 + 3 × 4 = 14
(2 + 3) × 4 = 20
0.1 + 0.2 = 0.3
200 + 10% = 220
200 × 10% = 20
200 ÷ 10% = 2000
```

---

## `CalculatorAccessCodePolicyTest.kt`

Checks:

- matching five-digit codes;
- non-matching codes;
- too-short code;
- too-long code;
- letters;
- non-ASCII digits;
- `00000`;
- leading zero.

---

## `CalculatorDeviceAuthenticatorTest.kt`

Checks the allowed authenticator set for:

```text
API 26–29
API 30–36
```

---

## `CalculatorCoverRecoveryManagerTest.kt`

Checks the recovery state machine:

- cancelled authentication;
- failed authentication;
- cancelled confirmation;
- successful reset;
- rejection of two parallel attempts;
- Cover Mode changing during authentication;
- stale callbacks;
- manager recreation;
- failure during reset;
- returning to IDLE after an error.

---

## `CalculatorAccessCodeManagerInstrumentedTest.kt`

Checks real Android Keystore verifier behavior.

In particular:

- only the configured code is accepted;
- incorrect five-digit values are rejected;
- leading zero is preserved.

---

## `CalculatorRecoveryInstrumentedTest.kt`

Checks the complete recovery flow with real Android components.

It verifies:

- return to Default;
- only the standard launcher alias remains active;
- calculator verifier removal;
- the old code can no longer be verified;
- Calculator history is preserved;
- user notes are preserved;
- authentication cancellation;
- confirmation cancellation;
- rollback on SharedPreferences failure;
- rollback on launcher-state failure;
- recovery after simulated process death;
- calculator reset is rejected while another Cover Mode is active.

---

## `HistoryRecoveryGestureInstrumentedTest.kt`

Checks the hidden seven-second gesture.

It verifies:

- the History header;
- blank header area;
- header padding;
- no activation on history rows;
- no activation on the clear button;
- one activation per hold;
- finger movement during the hold;
- early release;
- disabled recovery target.

---

# 29. Manual verification flow

Before release, Calculator Cover Mode should be manually checked as follows:

1. activate the mode;
2. create a code using normal digits;
3. create a code with a leading zero;
4. restart the device;
5. open Calculator from the launcher;
6. perform normal calculations;
7. test `%`;
8. test parentheses;
9. test negative numbers;
10. test division by zero;
11. test a long expression;
12. open history;
13. clear history;
14. enter an incorrect five-digit code;
15. verify that it did not appear in history;
16. enter the correct code;
17. open Bluetooth Disable;
18. press “HIDE”;
19. verify that Back does not return to Bluetooth Disable;
20. open Calculator again;
21. hold History for 7 seconds;
22. cancel system authentication;
23. verify that the mode was not reset;
24. repeat recovery;
25. successfully complete system verification;
26. cancel the final confirmation;
27. verify that the mode remains active;
28. repeat recovery;
29. confirm reset;
30. verify that the standard launcher has returned;
31. verify that calculator history was preserved.

The checks should be repeated at minimum:

- in the light theme;
- in the dark theme;
- in Russian;
- in English;
- on several supported Android versions;
- on a physical device.

---

# 30. Core mode invariants

The implementation must always preserve the following rules.

### Invariant 1

Calculator Cover Activity must not be considered ready without:

```text
CALCULATOR mode
+
CALCULATOR launcher
+
valid access verifier
```

### Invariant 2

The five-digit code is never stored in plaintext.

### Invariant 3

The Keystore key must not be exported from AndroidKeyStore.

### Invariant 4

An incorrect five-digit access candidate must not be written to Calculator history.

### Invariant 5

A normal calculation must never open MainActivity if the verifier does not match.

### Invariant 6

The hidden recovery gesture alone does not authorize a reset.

The following are required:

```text
gesture
+
system authentication
+
user confirmation
```

### Invariant 7

An old authentication callback must not authorize a new attempt.

### Invariant 8

Resetting Calculator Cover Mode must not delete Calculator history.

### Invariant 9

Cover Mode switching must be transactional and recoverable after failure.

### Invariant 10

After returning from MainActivity through “HIDE”, MainActivity must not remain accessible through Back.

---

# 31. Limitations

Calculator Cover Mode is a local mode.

It:

- does not use the Internet;
- does not use a server to verify the code;
- does not synchronize history;
- does not send calculations;
- does not send the access code;
- does not use an external mathematical service.

The five-digit code is a hidden-access mechanism for the main interface, not a replacement for device-level system authentication.

The number of possible five-digit combinations is limited to:

```text
00000–99999
```

There is no separate attempt counter or lockout after multiple incorrect attempts in the Calculator access flow.

Therefore, emergency reset is additionally protected by Android system authentication and does not depend on knowledge of the Calculator code.

---

# 32. Summary

Calculator Cover Mode consists of three largely independent subsystems:

```text
1. Full Calculator
2. Hidden access-code mechanism
3. Safe Cover Mode management and recovery
```

Calculation layer:

```text
ViewModel
→ Tokenizer
→ Parser
→ AST
→ Evaluator
→ Formatter
→ History
```

Access mechanism:

```text
5 digits
→ HMAC-SHA256
→ Android Keystore
→ equalsPressed()
→ CoverModeNavigator
→ MainActivity
```

Emergency recovery mechanism:

```text
History header
→ hold 7 sec
→ system authentication
→ confirmation
→ transactional reset
→ Default launcher
```

This separation allows Calculator Cover Mode to operate as a standalone calculator without mixing mathematical logic, launcher switching, secret verifier storage, and system access recovery.
