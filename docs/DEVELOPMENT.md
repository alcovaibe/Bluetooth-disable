# Development setup

## Требования

- Android Studio / Android SDK 36;
- JDK 17;
- Android 8.0+ тестовое устройство или совместимый эмулятор;
- ADB.

## 1. Сборка

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Debug APK создаётся стандартным Android Gradle Plugin в `app/build/outputs/apk/debug/`.

## 2. Установка APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

При обычной установке приложение должно показать состояние `DEVICE OWNER REQUIRED`. Это ожидаемое поведение.

## 3. Назначение Device Owner

Для dev-тестирования Google допускает назначение собственного DPC через ADB, если на устройстве отсутствуют аккаунты и нет конфликтующего владельца/профиля управления.

```bash
adb shell dpm set-device-owner \
  com.pulse.bluetoothdisable/.admin.AppDeviceAdminReceiver
```

Проверить владельца можно системной командой:

```bash
adb shell dpm list-owners
```

На разных версиях Android набор команд `dpm` может немного отличаться. Источником истины для самого приложения остаётся `DevicePolicyManager.isDeviceOwnerApp()`.

## 4. Функциональная проверка

После назначения Device Owner:

1. Запустить приложение.
2. Убедиться, что отображается `OFF`.
3. Включить Bluetooth вручную в Android.
4. Нажать `ВКЛЮЧИТЬ ЗАЩИТУ`.
5. Убедиться, что приложение показывает `PROTECTED` только после подтверждения ограничения.
6. Проверить, что Android не позволяет штатно использовать Bluetooth.
7. Закрыть приложение и запустить снова — состояние должно остаться `PROTECTED`.
8. Перезагрузить устройство — системная политика должна сохраниться.
9. Нажать `СНЯТЬ ЗАЩИТУ`.
10. Убедиться, что приложение показывает `OFF`, а Bluetooth снова доступен для ручного включения.

## 5. Что приложение сейчас не делает

Текущая версия не содержит:

- QR provisioning;
- пользовательского deprovisioning flow;
- автоматического восстановления включённого Bluetooth после снятия защиты;
- сетевых обновлений;
- аналитики;
- Bluetooth profile management;
- фонового сервиса.

Не используйте dev-сборку как окончательный provisioning-механизм на основном личном устройстве: корректный пользовательский onboarding и deprovisioning будут реализованы отдельными этапами.
