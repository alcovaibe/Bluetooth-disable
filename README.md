# Bluetooth Disable

Минимальный Android DPC для системной блокировки Bluetooth на полностью управляемом устройстве.

## Статус

Рабочая основа V1:

- Android 8.0+ (`minSdk 26`);
- `targetSdk 36`;
- Kotlin + Jetpack Compose;
- Device Owner / Device Policy Controller;
- включение защиты через `UserManager.DISALLOW_BLUETOOTH`;
- снятие защиты через `DevicePolicyManager.clearUserRestriction`;
- фактическое состояние Android Device Policy является источником истины;
- Android 12+ admin-integrated provisioning entry points;
- шаблон QR provisioning для fully managed enrollment;
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

Для production-развёртывания используется QR provisioning после factory reset. Android 12+ entry points уже реализованы; для финального QR ещё нужны подписанный release APK, стабильный публичный HTTPS URL и checksum конкретного APK.

Шаблон и процедура: [`docs/QR_PROVISIONING.md`](docs/QR_PROVISIONING.md).

## Безопасность

Release keystore, пароли и signing credentials не должны храниться в репозитории.
