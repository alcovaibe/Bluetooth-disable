# Bluetooth Disable

Минимальный Android DPC для системной блокировки Bluetooth на полностью управляемом устройстве.

## Статус

Первая рабочая основа V1:

- Android 8.0+ (`minSdk 26`);
- `targetSdk 36`;
- Kotlin + Jetpack Compose;
- Device Owner / Device Policy Controller;
- включение защиты через `UserManager.DISALLOW_BLUETOOTH`;
- снятие защиты через `DevicePolicyManager.clearUserRestriction`;
- фактическое состояние Android Device Policy является источником истины;
- без root, Shizuku, Magisk, Accessibility Service и фонового сервиса;
- без INTERNET permission и Bluetooth runtime permissions;
- без аналитики и телеметрии.

## Режимы

### NORMAL

Bluetooth работает штатно.

### PROTECTED

Device Owner применяет `UserManager.DISALLOW_BLUETOOTH`. Android системно запрещает использование Bluetooth до снятия ограничения.

## Сборка

```bash
./gradlew assembleDebug
```

Полная локальная проверка:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Device Owner для разработки

Обычная установка APK не делает приложение Device Owner.

Для тестового устройства без аккаунтов установите debug APK, затем выполните:

```bash
adb shell dpm set-device-owner \
  com.pulse.bluetoothdisable/.admin.AppDeviceAdminReceiver
```

После этого приложение сможет применять системную Bluetooth-политику.

Подробности: [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md).

## Provisioning для пользователей

QR provisioning после factory reset будет реализован отдельным этапом. Он пока не входит в текущую реализацию.

## Безопасность

Release keystore, пароли и signing credentials не должны храниться в репозитории.
