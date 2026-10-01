# Calculator Cover Mode

## Назначение

**Calculator Cover Mode** — режим маскировки Bluetooth Disable под обычное приложение-калькулятор.

После активации режима:

- в launcher вместо стандартного Bluetooth Disable отображается калькулятор;
- используется отдельное имя и иконка;
- запуск launcher-иконки открывает полноценный интерфейс калькулятора;
- основной интерфейс Bluetooth Disable напрямую пользователю не показывается;
- для перехода в основной интерфейс используется заранее настроенный пятизначный код;
- код вводится как обычное число и подтверждается кнопкой `=`;
- режим имеет отдельную историю вычислений;
- предусмотрено аварийное восстановление доступа при забытом коде;
- аварийное восстановление требует системной аутентификации Android;
- при сбросе маскировки история вычислений сохраняется.

Calculator Cover Mode не является статическим экраном или имитацией калькулятора. Это самостоятельный локальный калькулятор со своим парсером выражений, вычислителем, историей и пользовательским интерфейсом.

Основной код режима расположен в:

`app/src/main/java/com/pulse/bluetoothdisable/cover/calculator/`

---

# 1. Общая архитектура

Режим состоит из нескольких отдельных уровней:

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

Отдельно от вычислительной части работают:

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

Таким образом, логика вычислений не смешивается с механизмом маскировки, launcher, хранением кода доступа или аварийным восстановлением.

---

# 2. Пользовательский сценарий

## 2.1. Активация

Пользователь открывает основной Bluetooth Disable и выбирает:

```text
Изменить значок
        ↓
Калькулятор
        ↓
Подтверждение
        ↓
Создание пятизначного кода
        ↓
Повтор кода
        ↓
Активация Calculator Cover Mode
```

Сначала показывается `CalculatorCoverConfirmationDialog`.

После подтверждения открывается `CalculatorCoverSetupDialog`.

Пользователь вводит два значения:

- код доступа;
- повтор кода.

Код должен состоять ровно из пяти ASCII-цифр:

```text
00000
01234
58317
99999
```

> **Примечание:** код `58317` во всей этой документации используется только как пример кода доступа. Приложение не имеет предустановленного кода `58317`; реальный код задаёт сам пользователь при настройке режима.

Допустимы ведущие нули.

Не допускаются:

```text
1234
123456
12a45
١٢٣٤٥
```

После успешной проверки вызывается:

```kotlin
CoverModeManager.activateCalculator(code)
```

Менеджер:

1. проверяет корректность кода;
2. сохраняет защищённый verifier кода;
3. переключает launcher на Calculator;
4. устанавливает активный режим `CALCULATOR`;
5. завершает транзакцию переключения;
6. удаляет конфигурацию доступа предыдущего Cover Mode;
7. открывает `CalculatorCoverActivity`.

---

# 3. Launcher и запуск

Для калькулятора используется отдельный launcher alias:

```text
LauncherAliasCalculator
```

Он направляет запуск в:

```text
CalculatorCoverActivity
```

Сам `CalculatorCoverActivity` имеет:

```text
android:exported="false"
android:launchMode="singleTask"
```

Внешние приложения не должны напрямую запускать Activity калькулятора.

Экспортирован только launcher alias, поскольку Android Launcher должен иметь возможность запускать соответствующий компонент.

После активации `LauncherIconController` оставляет включённым только alias выбранного режима.

При переключении сначала включается новый alias, после чего отключаются остальные. Это сделано для того, чтобы приложение не оставалось без launcher-компонента в промежуточный момент переключения.

Для Android 12 и новее `CalculatorCoverActivity` использует отдельную тему:

```text
Theme.BluetoothDisable.Cover.Calculator
```

В ней системному Splash Screen назначается иконка калькулятора.

---

# 4. Вход в основной Bluetooth Disable

Основной способ входа скрыт внутри обычной операции калькулятора.

Пользователь вводит код, например:

```text
58317
```

и нажимает:

```text
=
```

`CalculatorViewModel.equalsPressed()` сначала проверяет, является ли текущее выражение допустимым пятизначным кодом.

Если выражение имеет формат:

```text
NNNNN
```

оно передаётся в:

```kotlin
CalculatorAccessCodeManager.verify()
```

Если verifier совпадает:

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

Перед переходом состояние текущего выражения калькулятора очищается.

`MainActivity` получает внутренний признак того, что она была открыта из конкретного Cover Mode.

Только в этом случае основной интерфейс показывает действие **«СКРЫТЬ»**.

---

# 5. Возврат обратно в калькулятор

При нажатии **«СКРЫТЬ»** основной интерфейс вызывает:

```kotlin
CoverModeNavigator.hideToCoverMode(...)
```

Для Calculator используется:

```text
CalculatorCoverActivity
```

Activity запускается с:

```text
FLAG_ACTIVITY_NEW_TASK
FLAG_ACTIVITY_CLEAR_TASK
```

В результате основной интерфейс удаляется из текущего task.

Это важно для маскировки:

```text
Calculator
    ↓
секретный код
    ↓
Bluetooth Disable
    ↓
СКРЫТЬ
    ↓
Calculator
```

После возврата нажатие Back не должно неожиданно показать Bluetooth Disable.

---

# 6. Вычислительная цепочка

Обычные вычисления проходят следующие этапы:

```text
Нажатия пользователя
        ↓
CalculatorViewModel
        ↓
строка выражения
        ↓
CalculatorEngine
        ↓
CalculatorTokenizer
        ↓
список токенов
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
строка результата
        ↓
CalculatorHistoryStore
```

Такое разделение позволяет держать UI отдельно от синтаксического анализа и вычислений.

---

# 7. Поддерживаемые математические операции

Калькулятор поддерживает:

- сложение;
- вычитание;
- умножение;
- деление;
- унарный минус;
- скобки;
- десятичные числа;
- проценты.

Поддерживаются символы:

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

Точка и запятая распознаются как десятичный разделитель.

Внутреннее представление десятичных чисел использует точку.

---

# 8. Приоритет операций

Парсер реализует стандартный порядок:

```text
1. Скобки / числа
2. Унарный минус
3. Процент
4. Умножение / деление
5. Сложение / вычитание
```

Например:

```text
2 + 3 × 4
```

вычисляется как:

```text
2 + 12 = 14
```

а:

```text
(2 + 3) × 4
```

как:

```text
5 × 4 = 20
```

---

# 9. Семантика процентов

Процент является постфиксной операцией.

Самостоятельно:

```text
50%
```

равно:

```text
0.5
```

Для умножения:

```text
200 × 10%
```

получается:

```text
20
```

Для деления:

```text
200 ÷ 10%
```

получается:

```text
2000
```

Для сложения и вычитания используется обычная калькуляторная процентная семантика относительно левого операнда.

Например:

```text
200 + 10%
```

интерпретируется как:

```text
200 + 200 × 0.10
```

и даёт:

```text
220
```

А:

```text
200 - 10%
```

даёт:

```text
180
```

---

# 10. Точность вычислений

В качестве числового типа используется:

```kotlin
BigDecimal
```

Поэтому операции с конечным десятичным представлением выполняются без обычных ошибок `Double`.

Например:

```text
0.1 + 0.2
```

возвращает:

```text
0.3
```

Большие целые значения также вычисляются точно.

Если обычное `BigDecimal.divide()` невозможно выполнить без бесконечного десятичного представления, используется:

```kotlin
MathContext.DECIMAL128
```

Деление на ноль перехватывается отдельно и превращается в контролируемую ошибку интерфейса.

---

# 11. Файлы Calculator Cover Mode

## 11.1. `CalculatorAccessCodePolicy.kt`

Определяет правила допустимого кода доступа.

### `CODE_LENGTH`

```kotlin
const val CODE_LENGTH = 5
```

Единая длина кода во всём режиме.

### `isValid(code)`

Проверяет, что:

- длина равна пяти;
- каждый символ находится между `'0'` и `'9'`.

Проверяются именно ASCII-цифры.

### `matches(code, confirmation)`

Проверяет одновременно:

1. корректность основного кода;
2. полное совпадение кода и подтверждения.

Используется при настройке Calculator Cover Mode.

---

# 11.2. `CalculatorAccessCodeManager.kt`

Отвечает за безопасное создание, хранение, проверку и удаление кода доступа.

Сам код в `SharedPreferences` не хранится.

Вместо него хранится:

```text
HMAC-SHA256(code)
```

HMAC вычисляется с ключом из:

```text
AndroidKeyStore
```

В preferences хранится только Base64-представление результата HMAC.

## `setCode(code)`

1. проверяет код через `CalculatorAccessCodePolicy`;
2. получает существующий ключ или создаёт новый;
3. вычисляет HMAC-SHA256;
4. кодирует результат в Base64;
5. синхронно сохраняет verifier в SharedPreferences.

Используется `commit()`, чтобы активация Cover Mode знала, что значение действительно записано до перехода к следующему этапу транзакции.

## `verify(code)`

Проверяет введённый пользователем код.

Последовательность:

1. проверяет формат кода;
2. получает сохранённый verifier;
3. декодирует Base64;
4. получает существующий ключ из AndroidKeyStore;
5. вычисляет HMAC для введённого значения;
6. сравнивает два массива через:

```kotlin
MessageDigest.isEqual()
```

При повреждённом verifier, отсутствующем ключе или ошибке Keystore возвращается `false`.

Исключения наружу не передаются.

## `hasCode()`

Проверяет, что одновременно:

- verifier существует;
- соответствующий ключ присутствует в AndroidKeyStore.

Используется `CoverModeManager.isCalculatorReady()`.

## `clearVerifier()`

Удаляет сохранённый verifier из SharedPreferences, но **не удаляет Keystore key**.

Такое разделение сделано специально для безопасной транзакции сброса Cover Mode.

Пока переход ещё не зафиксирован, старый ключ сохраняется, поэтому при ошибке можно восстановить предыдущий verifier из rollback snapshot.

## `clearCode()`

Полностью удаляет конфигурацию доступа:

```text
verifier + Keystore key
```

## `deleteKey()`

Удаляет ключ из AndroidKeyStore.

Ошибки удаления подавляются, поскольку verifier к этому моменту уже удалён и раскрывать внутренние детали Keystore пользователю не требуется.

## `sign(key, code)`

Внутренний метод вычисления:

```text
HMAC-SHA256
```

для UTF-8 представления кода.

## `getOrCreateKey()`

Возвращает существующий ключ либо создаёт новый.

## `existingKey()`

Читает SecretKey из `AndroidKeyStore`.

## `loadKeyStore()`

Открывает Android Keystore и загружает его состояние.

## `generateKey()`

Создаёт новый HMAC-SHA256 key с назначениями:

```text
PURPOSE_SIGN
PURPOSE_VERIFY
```

Ключ остаётся внутри Android Keystore.

---

# 11.3. `CalculatorCoverDialogs.kt`

Содержит интерфейс первоначальной настройки режима.

## `CalculatorCoverConfirmationDialog()`

Обёртка над общим:

```kotlin
CoverModeConfirmationDialog
```

Показывает предупреждение перед включением режима Calculator.

При подтверждении MainActivity переходит к настройке кода.

## `CalculatorCoverSetupDialog()`

Основное окно настройки.

Хранит в Compose-state:

```text
code
confirmation
setupFailed
```

В реальном времени вычисляются:

```text
codeValid
confirmationValid
matches
```

Кнопка продолжения активна только когда:

- оба значения корректны;
- оба значения полностью совпадают.

При подтверждении вызывается:

```kotlin
onComplete(code)
```

Если активация Cover Mode не удалась, показывается ошибка настройки.

## `AccessCodeField()`

Общее поле ввода кода.

Использует:

```text
PasswordVisualTransformation
KeyboardType.NumberPassword
```

Поддерживает отображение ошибок:

- неверная длина;
- несовпадение повторного кода.

## `sanitizeCode()`

Фильтрует пользовательский ввод.

Оставляет только:

```text
0–9
```

и максимум пять символов.

---

# 11.4. `CalculatorCoverActivity.kt`

Главная Android Activity режима Calculator.

Она связывает:

- UI калькулятора;
- ViewModel;
- системную тему;
- навигацию;
- механизм аварийного восстановления.

## `attachBaseContext()`

Передаёт Context через:

```kotlin
LanguageManager.wrapContext()
```

Благодаря этому режим использует язык, выбранный в Bluetooth Disable.

## `onCreate()`

Основная точка инициализации.

Сначала выполняется:

```kotlin
CoverModeManager(this).isCalculatorReady()
```

Calculator считается готовым только если одновременно:

```text
activeMode == CALCULATOR
launcherStyle == CALCULATOR
accessCode существует
```

Если состояние повреждено или неполно, Calculator Cover Activity не продолжает обычный запуск и возвращает пользователя в `MainActivity`.

Далее:

1. включается edge-to-edge;
2. создаётся `CoverModeManager`;
3. создаётся `CoverRecoveryManager`;
4. создаётся `CoverDeviceAuthenticator`;
5. получается `CalculatorViewModel`;
6. запускается Compose UI;
7. применяется `CoverTheme`;
8. отображается `CalculatorScreen`.

`onUnlock` вызывает:

```kotlin
CoverModeNavigator.openMainFromCover(
    this,
    CoverMode.CALCULATOR
)
```

## `beginRecovery()`

Запускает аварийный сброс маскировки.

Метод:

1. проверяет, что Activity находится в `RESUMED`;
2. просит `CoverRecoveryManager` начать новую попытку;
3. выполняет haptic feedback;
4. запускает системную аутентификацию;
5. передаёт результат обратно в Recovery Manager.

Если системная аутентификация недоступна, попытка отменяется и показывается Toast.

## `resetCover()`

Вызывается после:

```text
7 секунд удержания
→ успешная системная аутентификация
→ подтверждение пользователем
```

`CoverRecoveryManager.confirmReset()` вызывает:

```kotlin
CoverModeManager.resetCalculatorCover()
```

После успешного сброса открывается обычный Bluetooth Disable через:

```kotlin
CoverModeNavigator.openDefaultMain()
```

## `onResume()`

Устанавливает:

```text
resumed = true
```

Это разрешает запуск hidden recovery gesture.

## `onPause()`

Отключает recovery gesture.

Если пользователь покинул Activity в момент окна подтверждения, операция сброса отменяется.

## `onDestroy()`

Отменяет незавершённое recovery-состояние и закрывает `CoverDeviceAuthenticator`.

## `onNewIntent()`

Поскольку Activity работает в `singleTask`, новый launcher-запуск может прийти через `onNewIntent()`.

В этом случае:

- отменяется старый recovery flow;
- очищается текущее выражение калькулятора.

---

# 11.5. `CalculatorScreen.kt`

Основной Compose-интерфейс калькулятора.

Получает:

```kotlin
CalculatorViewModel
onUnlock
recoveryEnabled
onRecoveryHold
```

## `CalculatorScreen()`

Создаёт весь основной экран.

Содержит:

- кнопку истории;
- drawer истории;
- предыдущую строку вычисления;
- текущее выражение/результат;
- сообщения об ошибках;
- клавиатуру калькулятора.

Для русского языка отображаемая десятичная точка заменяется на запятую.

Внутреннее математическое выражение при этом остаётся нормализованным.

Для длинных результатов автоматически уменьшается размер текста:

```text
до 20 символов  → 52sp
21–34           → 36sp
более 34        → 28sp
```

Экран поддерживает горизонтальную прокрутку длинного выражения.

Обработка кнопок:

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

Если `equalsPressed()` возвращает `true`, вызывается `onUnlock()`.

## `CalculatorKeypad()`

Строит адаптивную клавиатуру.

Размер кнопок рассчитывается относительно доступной ширины и ограничивается диапазоном:

```text
56dp–92dp
```

Клавиатура разделена на:

- блок обычных клавиш;
- вертикальный блок операторов.

## `CalculatorThreeKeyRow()`

Создаёт ряд из трёх круглых кнопок.

## `CalculatorCircleKey()`

Рисует обычную круглую кнопку.

Кнопка `C` визуально выделяется через:

```text
errorContainer
```

Остальные используют:

```text
surfaceVariant
```

## `CalculatorOperatorRail()`

Создаёт вертикальную панель:

```text
÷
×
−
+
=
```

Кнопка `=` отдельно выделяется основным цветом Material Theme.

---

# 11.6. `CalculatorViewModel.kt`

Центральная логика пользовательского взаимодействия.

ViewModel не занимается непосредственно Compose-разметкой.

Она управляет:

- текущим выражением;
- результатом;
- ошибкой;
- историей;
- состоянием после вычисления;
- проверкой секретного кода.

## `CalculatorUiState`

Содержит:

### `expression`

Текущее математическое выражение.

### `previousExpression`

Строка предыдущего вычисления, например:

```text
2+2 =
```

### `display`

Значение, отображаемое крупным текстом.

### `error`

Одна из ошибок Calculator Engine либо `null`.

### `afterResult`

Показывает, что последнее действие завершилось нажатием `=`.

Используется для корректного поведения следующего ввода.

### `history`

Список сохранённых вычислений.

---

## `inputDigit(digit)`

Добавляет цифру.

Если перед этим был получен результат, старое выражение заменяется новым вводом.

---

## `inputDecimal()`

Добавляет десятичный разделитель.

Метод:

- не позволяет добавить две точки в одно число;
- после оператора автоматически создаёт `0.`;
- в пустом выражении создаёт `0.`;
- после `(` также создаёт `0.`.

---

## `inputOperator(operator)`

Обрабатывает:

```text
+
-
×
÷
```

Учитывает несколько состояний.

После результата:

```text
2 + 2 = 4
```

нажатие `+` создаёт:

```text
4+
```

В начале выражения разрешён только `-`, поскольку он может быть унарным минусом.

Метод также:

- заменяет предыдущий бинарный оператор новым;
- разрешает унарный минус после оператора;
- корректирует число, заканчивающееся точкой;
- разрешает оператор после числа, `%` или `)`.

---

## `inputPercent()`

Добавляет `%`, если выражение заканчивается:

- цифрой;
- закрывающей скобкой.

После уже вычисленного результата `%` сразу не добавляется.

---

## `inputParenthesis()`

Одна кнопка управляет обеими скобками.

Открывающая `(` добавляется:

- в пустое выражение;
- после другой `(`;
- после бинарного оператора.

Закрывающая `)` добавляется только если:

- открытых скобок больше, чем закрытых;
- перед ней находится число, `%` или `)`.

---

## `backspace()`

Удаляет последний символ выражения.

Также:

- сбрасывает ошибку;
- снимает состояние `afterResult`;
- возвращает display к `0`, если выражение стало пустым.

---

## `clear()`

Полностью очищает текущее состояние вычисления, но **не удаляет историю**.

---

## `clearHistory()`

Удаляет историю из `CalculatorHistoryStore` и обновляет UI-state.

---

## `equalsPressed()`

Ключевой метод режима.

Он одновременно выполняет две функции:

1. обычное вычисление;
2. скрытую проверку кода доступа.

Сначала:

```kotlin
CalculatorAccessCodePolicy.isValid(raw)
```

определяет, выглядит ли введённое значение как пятизначный код.

Если формат подходит и:

```kotlin
CalculatorAccessCodeManager.verify(raw)
```

возвращает `true`, ViewModel:

- очищает текущий экран;
- возвращает `true`.

`CalculatorScreen` воспринимает это как команду открыть основной Bluetooth Disable.

Если код не совпал, выражение обрабатывается как обычное число/математическое выражение.

При этом **любое выражение, состоящее ровно из пяти цифр, не записывается в историю**, даже если оно не является правильным кодом.

Это предотвращает появление введённых пятизначных кандидатов доступа в истории калькулятора.

Для обычного выражения:

1. запускается `CalculatorEngine.evaluate()`;
2. результат форматируется;
3. выражение сохраняется в историю;
4. UI переходит в состояние `afterResult`.

При ошибке устанавливается соответствующий `CalculatorEngineError`.

Возвращаемое значение:

```text
true  → код принят, требуется открыть Bluetooth Disable
false → обычное вычисление или ошибка
```

---

## `updateExpression()`

Внутренний вспомогательный метод.

Синхронно обновляет:

- expression;
- previousExpression;
- display;
- error;
- afterResult.

---

## `isBinaryOperator()`

Проверяет, относится ли символ к:

```text
+ - × ÷
```

---

# 11.7. `CalculatorHistoryStore.kt`

Отвечает за локальное хранение истории.

## `CalculatorHistoryEntry`

Содержит:

```text
expression
result
```

Например:

```text
expression = "200+10%"
result = "220"
```

## `entries()`

Читает историю из SharedPreferences.

Количество записей принудительно ограничивается диапазоном:

```text
0..50
```

Повреждённая неполная запись, у которой отсутствует expression или result, пропускается.

## `add(expression, result)`

Добавляет новую запись в начало списка.

После этого история обрезается до:

```text
MAX_ENTRIES = 50
```

SharedPreferences полностью перезаписывается актуальным списком.

Используется синхронный commit.

## `clear()`

Полностью очищает историю.

## `expressionKey(index)`

Создаёт ключ:

```text
expression_0
expression_1
...
```

## `resultKey(index)`

Создаёт:

```text
result_0
result_1
...
```

История является отдельным пользовательским хранилищем и не удаляется при обычном аварийном сбросе Calculator Cover Mode.

---

# 11.8. `CalculatorHistoryDrawer.kt`

Compose-интерфейс истории вычислений.

## `CalculatorHistoryDrawer()`

Создаёт боковую панель шириной:

```text
290dp
```

Панель содержит:

1. заголовок «История»;
2. список вычислений;
3. кнопку очистки.

Если история отсутствует, отображается сообщение о пустой истории.

Для списка используется `LazyColumn`.

### Hidden recovery target

Заголовок drawer является скрытой зоной аварийного восстановления:

```kotlin
.coverRecoveryHold(...)
```

Recovery включён только если:

- режим восстановления разрешён Activity;
- drawer открыт;
- drawer больше не находится в процессе анимации.

Удержание заголовка в течение 7 секунд инициирует recovery flow.

Строки истории и кнопка очистки не являются recovery-зоной.

## `CalculatorHistoryRow()`

Формирует строку:

```text
выражение = результат
```

Для русского интерфейса точка заменяется на запятую.

Для короткой строки используется одна строка текста.

Если длина превышает установленный предел, допускаются две строки.

Переполнение завершается ellipsis.

---

# 11.9. `CalculatorEngine.kt`

Публичная точка входа вычислительного движка калькулятора.

## `CalculatorEvaluation`

Результат работы движка.

### `Success`

Содержит:

```kotlin
BigDecimal
```

### `Failure`

Содержит:

```kotlin
CalculatorEngineError
```

## `CalculatorEngineError`

В настоящее время:

```text
DIVISION_BY_ZERO
INVALID_EXPRESSION
```

## `evaluate(expression)`

Выполняет полный pipeline:

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

Пустое выражение сразу считается некорректным.

Ошибки парсера и арифметики не выходят наружу исключениями.

Они преобразуются в контролируемый:

```text
CalculatorEvaluation.Failure
```

---

# 11.10. `CalculatorTokenizer.kt`

Преобразует исходную строку в токены.

## `CalculatorTokenType`

Определены:

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

Содержит:

```text
type
text
```

Поле `text` в основном используется для чисел.

## `CalculatorParseException`

Единый тип исключения для некорректного синтаксиса.

## `CalculatorTokenizer.tokenize()`

Проходит строку посимвольно.

Поддерживает:

### Числа

```text
0–9
```

### Десятичные разделители

```text
.
,
```

Запятая нормализуется в точку.

### Минус

```text
-
−
```

### Умножение

```text
×
*
```

### Деление

```text
÷
/
```

### Другие символы

```text
+
%
(
)
```

Пробелы игнорируются.

Любой неизвестный символ вызывает `CalculatorParseException`.

В одном числе разрешён только один десятичный разделитель.

После всех токенов автоматически добавляется:

```text
EOF
```

---

# 11.11. `CalculatorAst.kt`

Определяет внутреннее абстрактное синтаксическое дерево — AST.

## `CalculatorNode`

Общий sealed interface для всех узлов.

## `NumberNode`

Хранит числовое значение:

```kotlin
BigDecimal
```

## `UnaryMinusNode`

Представляет:

```text
-x
```

## `PercentNode`

Представляет:

```text
x%
```

## `BinaryNode`

Представляет бинарную операцию:

```text
left operator right
```

## `BinaryOperator`

Поддерживает:

```text
ADD
SUBTRACT
MULTIPLY
DIVIDE
```

AST позволяет отделить синтаксис выражения от непосредственного вычисления.

---

# 11.12. `CalculatorParser.kt`

Преобразует список токенов в AST.

Используется recursive descent parser.

## `parse()`

Начинает разбор с уровня сложения/вычитания.

После завершения требует `EOF`.

Если остаются необработанные токены, выражение считается некорректным.

## `parseAddSubtract()`

Разбирает:

```text
+
-
```

Операндами являются результаты `parseMultiplyDivide()`.

Это обеспечивает более низкий приоритет сложения и вычитания.

## `parseMultiplyDivide()`

Разбирает:

```text
×
÷
```

Использует `parsePercent()`.

## `parsePercent()`

Обрабатывает постфиксный:

```text
%
```

Технически поддерживается последовательное применение `%`.

## `parseUnary()`

Реализует унарный минус.

Например:

```text
-5
2 × -3
(-5)
```

## `parsePrimary()`

Обрабатывает:

- число;
- выражение в скобках.

Открывающая скобка требует соответствующей закрывающей.

Текст NUMBER преобразуется в `BigDecimal`.

## `match(type)`

Проверяет текущий токен.

При совпадении сдвигает текущую позицию на один элемент.

## `current()`

Возвращает текущий токен парсера.

---

# 11.13. `CalculatorEvaluator.kt`

Вычисляет готовое AST.

## `CalculatorDivisionByZeroException`

Отдельное внутреннее исключение для деления на ноль.

Позволяет `CalculatorEngine` отличать эту ситуацию от обычной ошибки выражения.

## `CalculatorEvaluator.evaluate(node)`

Рекурсивно вычисляет любой тип узла.

### NumberNode

Возвращает число.

### UnaryMinusNode

Вычисляет operand и меняет знак.

### PercentNode

Делит operand на:

```text
100
```

### BinaryNode

Передаёт обработку в `evaluateBinary()`.

## `evaluateBinary()`

Обрабатывает бинарные операции.

Особый случай существует для:

```text
A + B%
A - B%
```

В этом случае вычисляется:

```text
delta = A × B%
```

После чего:

```text
A + delta
```

или:

```text
A - delta
```

Для остальных операций правый operand вычисляется обычным образом.

## `divide()`

Сначала проверяет деление на ноль.

После этого пытается выполнить точное:

```kotlin
left.divide(right)
```

Если десятичное представление бесконечно, используется:

```kotlin
MathContext.DECIMAL128
```

---

# 11.14. `CalculatorFormatter.kt`

Форматирует математические результаты для UI и истории.

## `format(value)`

Если значение математически равно нулю:

```text
0
```

возвращается именно строка `"0"`.

Далее вызывается:

```kotlin
stripTrailingZeros()
```

Поэтому:

```text
2.0000
```

отображается как:

```text
2
```

Обычное десятичное представление используется, пока его длина не превышает:

```text
60 символов
```

Для очень длинных значений применяется:

```kotlin
toEngineeringString()
```

## `localize(value, useComma)`

Для локалей, где интерфейс калькулятора использует запятую, заменяет:

```text
.
```

на:

```text
,
```

Это только форматирование отображения.

Внутренние выражения и вычисления остаются нормализованными.

---

# 12. Аварийное восстановление

Если пользователь забыл пятизначный код, режим предусматривает независимый путь восстановления.

Сценарий:

```text
Открыть Calculator
        ↓
Открыть History
        ↓
Удерживать заголовок History 7 секунд
        ↓
Haptic feedback
        ↓
Системная аутентификация Android
        ↓
Подтверждение сброса
        ↓
Calculator Cover Mode → DEFAULT
```

Само удержание **не сбрасывает режим**.

Оно только запускает системную аутентификацию.

---

# 13. `CoverRecoveryGesture.kt`

Общий компонент Cover Mode, используемый калькулятором.

## `coverRecoveryHold()`

Compose Modifier, отслеживающий непрерывное удержание в течение:

```text
7000 мс
```

Особенности:

- допускается небольшое движение пальца;
- не требуется непоглощённый pointer event;
- работает внутри drawer и других Compose-компонентов;
- отпускание пальца до 7 секунд отменяет таймер;
- после срабатывания одного удержания callback не вызывается повторно до следующего gesture.

Это повышает устойчивость скрытого жеста на разных OEM-устройствах.

---

# 14. `CoverRecoveryManager.kt`

Общая state machine аварийного восстановления.

Для Calculator по умолчанию используется:

```text
modeToRecover = CALCULATOR
```

Состояния:

```text
IDLE
AUTHENTICATING
CONFIRMING
RESETTING
```

## `begin()`

Запускает попытку только если:

- текущее состояние `IDLE`;
- активен правильный Cover Mode.

Возвращает уникальный номер попытки.

## `authenticationSucceeded(id)`

Принимает успешную аутентификацию только если:

- ID соответствует текущей попытке;
- состояние всё ещё `AUTHENTICATING`;
- активный Cover Mode не изменился.

После этого состояние становится:

```text
CONFIRMING
```

## `authenticationRejected(id)`

Отменяет текущую попытку при неуспешной аутентификации.

## `cancel()`

Увеличивает внутренний ID попытки и возвращает state в `IDLE`.

Благодаря увеличению ID запоздавший callback старого BiometricPrompt не может авторизовать следующую попытку.

## `confirmReset()`

Разрешает сброс только после состояния:

```text
CONFIRMING
```

и только если всё ещё активен Calculator Cover Mode.

После этого вызывает:

```kotlin
resetCover()
```

В Calculator это:

```kotlin
CoverModeManager.resetCalculatorCover()
```

После завершения менеджер всегда возвращается в `IDLE`.

---

# 15. `CoverDeviceAuthenticator.kt`

Общая обёртка над AndroidX `BiometricPrompt`.

Не хранит:

- PIN;
- пароль;
- графический ключ;
- биометрические данные;
- успешный auth token.

Вся проверка выполняется Android.

## Android API 26–29

Используется:

```text
BIOMETRIC_WEAK | DEVICE_CREDENTIAL
```

## Android API 30+

Используется:

```text
BIOMETRIC_STRONG | DEVICE_CREDENTIAL
```

Таким образом пользователь может подтвердить личность совместимым системным способом.

## `authenticate()`

Проверяет доступность аутентификации через:

```kotlin
BiometricManager.canAuthenticate()
```

После этого запускает системный prompt.

Одновременно допускается только одна активная попытка.

## `close()`

Отменяет активный prompt и запрещает новые попытки для уничтожаемого экземпляра.

---

# 16. Сброс Calculator Cover Mode

После успешной системной проверки и дополнительного подтверждения вызывается:

```kotlin
CoverModeManager.resetCalculatorCover()
```

Операция:

1. убеждается, что текущий режим действительно Calculator;
2. начинает транзакцию перехода в `DEFAULT`;
3. удаляет calculator verifier;
4. переключает launcher на стандартный;
5. проверяет, что включён только стандартный launcher alias;
6. фиксирует `CoverMode.DEFAULT`;
7. очищает rollback journal;
8. только после commit point удаляет старый HMAC key.

При этом **CalculatorHistoryStore не очищается**.

Также не удаляются пользовательские данные Notes и Gallery.

---

# 17. Транзакционность переключения Cover Mode

Переключение launcher и секретной конфигурации нельзя выполнять как набор независимых несвязанных операций.

Например, процесс Android может быть завершён между:

```text
сохранением кода
```

и:

```text
включением launcher alias
```

Поэтому `CoverModeManager` использует rollback journal.

До начала перехода сохраняется snapshot:

```text
previous active mode
previous launcher style
launcher hidden state
calculator access verifier
calendar access verifier
notes access verifier
gallery access verifier
```

Далее в `CoverModeStore` записываются:

```text
pending mode
rollback snapshot
```

Только после этого изменяется реальная конфигурация.

---

# 18. Commit point

Для Calculator activation последовательность упрощённо выглядит так:

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

`clearPending()` является точкой, после которой новая конфигурация считается устойчиво зафиксированной.

Если до этой точки возникает исключение, `CoverModeManager` вызывает восстановление snapshot.

---

# 19. Восстановление после process death

При старте приложения:

```kotlin
BluetoothDisablerApplication
```

вызывает:

```kotlin
CoverModeManager.recoverInterruptedSetup()
```

Если существует незавершённая транзакция:

```text
pendingMode != null
```

менеджер читает rollback snapshot и возвращает предыдущее согласованное состояние.

Для Calculator дополнительно проверяется:

```kotlin
isCalculatorReady()
```

То есть должны одновременно существовать:

```text
CoverMode.CALCULATOR
LauncherStyle.CALCULATOR
валидный access verifier + Keystore key
```

Если согласованность нарушена, приложение восстанавливается в Default.

---

# 20. Почему ключ удаляется позже verifier

При аварийном сбросе используется особый порядок:

```text
clearVerifier()
        ↓
переключение launcher
        ↓
изменение active mode
        ↓
commit point
        ↓
deleteKey()
```

Это намеренное поведение.

Если удалить Keystore key сразу, а следующая операция транзакции завершится ошибкой, старый verifier можно будет восстановить из snapshot, но проверить его уже будет невозможно.

Поэтому старый ключ сохраняется до успешного commit point.

---

# 21. `CoverModeNavigator.kt`

Отвечает за безопасные переходы между настоящим приложением и Cover Mode.

## `openMainFromCover()`

Перед открытием MainActivity:

- проверяет, что mode относится к Cover Mode;
- убеждается, что он всё ещё является активным.

В Intent помещается внутренний origin:

```text
INTERNAL_COVER_ORIGIN
```

## `coverOrigin()`

MainActivity использует этот метод, чтобы определить, действительно ли была открыта из активного Cover Mode.

Просто передать произвольное старое значение Intent недостаточно: активный режим перепроверяется.

## `hideToCoverMode()`

Возвращает пользователя в Calculator и очищает task.

## `openDefaultMain()`

Используется после аварийного сброса.

Метод требует:

```text
activeMode == DEFAULT
```

и открывает новый чистый task MainActivity.

---

# 22. `CoverSystemUi.kt`

Calculator использует общий системный UI Cover Modes.

## `enableCoverEdgeToEdge()`

Включает edge-to-edge и делает прозрачными:

- status bar;
- navigation bar;
- navigation bar divider.

На Android 10+ отключается принудительный navigation bar contrast.

## `CoverTheme()`

Получает пользовательскую тему Bluetooth Disable через `ThemeManager`.

Поддерживаются:

```text
LIGHT
DARK
SYSTEM
```

Цвет иконок status/navigation bar переключается в соответствии с текущей светлой или тёмной темой.

В результате Calculator Cover Mode использует тот же принцип отображения системных панелей, что и основной интерфейс.

---

# 23. История и секретный код

Calculator history и access code — независимые подсистемы.

```text
calculator_history_preferences
```

содержит обычную историю вычислений.

```text
calculator_access_code_preferences
```

содержит только HMAC verifier секретного кода.

Сам пятизначный код не записывается в историю при вводе через Calculator.

Кроме того, любое самостоятельное пятизначное число рассматривается как потенциальный access candidate и не сохраняется в истории даже при неверном коде.

Пример:

```text
введено: 58318
пример настроенного кода: 58317
```

Calculator может обработать `58318` как обычное число, но запись:

```text
58318 = 58318
```

в history не добавляется.

---

# 24. Ошибки

Calculator Engine различает две пользовательские категории ошибок.

## `DIVISION_BY_ZERO`

Например:

```text
1 ÷ 0
```

## `INVALID_EXPRESSION`

Например:

```text
2 + × 3
(2 + 3
1.2.3
%
2 ÷
```

Исключения parser/evaluator не должны попадать непосредственно в UI.

Пользователь получает локализованную ошибку через `CalculatorUiState.error`.

---

# 25. Защита от некорректного состояния

`CalculatorCoverActivity` не доверяет только факту своего запуска.

Перед отображением калькулятора проверяется:

```text
active mode
+
launcher style
+
наличие verifier и Keystore key
```

Если любой элемент отсутствует, Activity возвращается к MainActivity.

Это предотвращает работу неполностью настроенного режима после:

- сбоя;
- повреждённого preferences;
- незавершённого перехода;
- старой конфигурации;
- потери Keystore key.

---

# 26. Lifecycle безопасности recovery

Recovery flow намеренно не переживает пересоздание Activity как подтверждённая операция.

Разрешение на reset хранится только в памяти.

Новый экземпляр `CoverRecoveryManager` начинает с:

```text
IDLE
```

Даже если предыдущий экземпляр уже успешно прошёл аутентификацию, новый экземпляр требует новую системную проверку.

При:

```text
onPause
onDestroy
onNewIntent
```

незавершённые операции отменяются.

---

# 27. Что сохраняется при аварийном сбросе

После успешного Calculator recovery:

Удаляется:

```text
Calculator Cover Mode
Calculator access verifier
Calculator access Keystore key
Calculator launcher alias как активный alias
```

Восстанавливается:

```text
CoverMode.DEFAULT
стандартная launcher-иконка
стандартное имя приложения
```

Сохраняется:

```text
Calculator history
Calendar notes
Notes user data
Gallery user data
остальные пользовательские данные приложения
```

---

# 28. Тестирование

## `CalculatorEngineTest.kt`

Проверяет:

- сложение;
- вычитание;
- умножение;
- деление;
- приоритет операций;
- скобки;
- унарный минус;
- отрицательное число после умножения;
- десятичную точность;
- процентные операции;
- деление на ноль;
- большие целые числа;
- некорректные выражения.

Примеры проверяемого поведения:

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

Проверяет:

- совпадающие пятизначные коды;
- несовпадающие коды;
- слишком короткий код;
- слишком длинный код;
- буквы;
- не-ASCII цифры;
- `00000`;
- ведущий ноль.

---

## `CalculatorDeviceAuthenticatorTest.kt`

Проверяет набор допустимых authenticators для:

```text
API 26–29
API 30–36
```

---

## `CalculatorCoverRecoveryManagerTest.kt`

Проверяет state machine recovery:

- отменённую аутентификацию;
- неудачную аутентификацию;
- отмену confirmation;
- успешный reset;
- запрет двух параллельных попыток;
- изменение Cover Mode во время аутентификации;
- устаревшие callbacks;
- recreation manager;
- ошибку во время reset;
- возврат в IDLE после ошибки.

---

## `CalculatorAccessCodeManagerInstrumentedTest.kt`

Проверяет реальную работу Android Keystore verifier.

В частности:

- принимается только настроенный код;
- неверные пятизначные значения отвергаются;
- ведущий ноль не теряется.

---

## `CalculatorRecoveryInstrumentedTest.kt`

Проверяет полный recovery с реальными Android-компонентами.

Проверяется:

- возврат к Default;
- единственный активный стандартный launcher alias;
- удаление calculator verifier;
- невозможность проверить старый код;
- сохранение Calculator history;
- сохранение пользовательских заметок;
- отмена authentication;
- отмена confirmation;
- rollback при ошибке SharedPreferences;
- rollback при ошибке launcher state;
- восстановление после имитации process death;
- невозможность calculator reset при активном другом Cover Mode.

---

## `HistoryRecoveryGestureInstrumentedTest.kt`

Проверяет скрытый семисекундный gesture.

Проверяются:

- заголовок History;
- пустая область header;
- padding header;
- отсутствие срабатывания на строках истории;
- отсутствие срабатывания на кнопке очистки;
- одно срабатывание за одно удержание;
- движение пальца во время удержания;
- раннее отпускание;
- выключенный recovery target.

---

# 29. Ручной сценарий проверки

Перед релизом Calculator Cover Mode рекомендуется проверить вручную:

1. активировать режим;
2. создать код с обычными цифрами;
3. создать код с ведущим нулём;
4. перезапустить устройство;
5. открыть Calculator из launcher;
6. выполнить обычные вычисления;
7. проверить `%`;
8. проверить скобки;
9. проверить отрицательные числа;
10. проверить деление на ноль;
11. проверить длинное выражение;
12. открыть историю;
13. очистить историю;
14. ввести неверный пятизначный код;
15. убедиться, что он не появился в истории;
16. ввести правильный код;
17. открыть Bluetooth Disable;
18. нажать «СКРЫТЬ»;
19. убедиться, что Back не возвращает Bluetooth Disable;
20. снова открыть Calculator;
21. удерживать History 7 секунд;
22. отменить системную аутентификацию;
23. проверить, что режим не сбросился;
24. повторить recovery;
25. успешно пройти системную проверку;
26. отменить финальное подтверждение;
27. проверить сохранение режима;
28. повторить recovery;
29. подтвердить reset;
30. убедиться, что вернулся стандартный launcher;
31. убедиться, что история калькулятора сохранилась.

Проверки следует повторить как минимум:

- в светлой теме;
- в тёмной теме;
- на русском языке;
- на английском языке;
- на нескольких поддерживаемых версиях Android;
- на физическом устройстве.

---

# 30. Основные инварианты режима

Реализация должна всегда сохранять следующие правила.

### Инвариант 1

Calculator Cover Activity не должна считаться готовой без:

```text
CALCULATOR mode
+
CALCULATOR launcher
+
валидного access verifier
```

### Инвариант 2

Пятизначный код никогда не хранится в открытом виде.

### Инвариант 3

Keystore key не должен экспортироваться из AndroidKeyStore.

### Инвариант 4

Неверный пятизначный access candidate не должен попадать в Calculator history.

### Инвариант 5

Обычное вычисление никогда не должно открыть MainActivity, если verifier не совпал.

### Инвариант 6

Hidden recovery gesture сам по себе не даёт права на reset.

Необходимо:

```text
gesture
+
system authentication
+
user confirmation
```

### Инвариант 7

Старый authentication callback не должен подтверждать новую попытку.

### Инвариант 8

Reset Calculator Cover Mode не должен удалять Calculator history.

### Инвариант 9

Переключение Cover Mode должно быть транзакционным и восстанавливаемым после сбоя.

### Инвариант 10

После возврата из MainActivity через «СКРЫТЬ» MainActivity не должна оставаться доступной через Back.

---

# 31. Ограничения

Calculator Cover Mode является локальным режимом.

Он:

- не использует Интернет;
- не использует сервер для проверки кода;
- не синхронизирует историю;
- не отправляет вычисления;
- не отправляет access code;
- не использует сторонний математический сервис.

Пятизначный код является механизмом скрытого доступа к основному интерфейсу, а не заменой системной аутентификации устройства.

Число возможных пятизначных комбинаций ограничено диапазоном:

```text
00000–99999
```

Отдельного счётчика или блокировки после нескольких неверных попыток в Calculator access flow нет.

Поэтому аварийный reset дополнительно защищён системной аутентификацией Android и не зависит от знания Calculator-кода.

---

# 32. Итог

Calculator Cover Mode состоит из трёх практически независимых подсистем:

```text
1. Полноценный Calculator
2. Скрытый access-code механизм
3. Безопасное управление Cover Mode и recovery
```

Вычислительная часть:

```text
ViewModel
→ Tokenizer
→ Parser
→ AST
→ Evaluator
→ Formatter
→ History
```

Механизм доступа:

```text
5 digits
→ HMAC-SHA256
→ Android Keystore
→ equalsPressed()
→ CoverModeNavigator
→ MainActivity
```

Механизм аварийного восстановления:

```text
History header
→ hold 7 sec
→ system authentication
→ confirmation
→ transactional reset
→ Default launcher
```

Такое разделение позволяет Calculator Cover Mode работать как самостоятельный калькулятор, не смешивая математическую логику, launcher switching, хранение секретного verifier и системное восстановление доступа.
