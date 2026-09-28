# QR provisioning

**Language:** [Russian](QR_PROVISIONING.md) | [English](QR_PROVISIONING_EN.md)

Bluetooth Disable is intended to be provisioned as a **fully managed Device Owner** after a factory reset.

Android recommends QR enrollment for fully managed and dedicated devices. The QR payload must point to the signed APK and include a checksum so Android Setup Wizard can verify the downloaded package.

## Android 12+

The application implements the two admin-integrated provisioning entry points required on Android 12 and newer:

- `android.app.action.GET_PROVISIONING_MODE`;
- `android.app.action.ADMIN_POLICY_COMPLIANCE`.

The first selects `PROVISIONING_MODE_FULLY_MANAGED_DEVICE`. The second completes provisioning only after Android confirms that this package is the Device Owner.

## Release APK checksum

After building the exact APK that will be hosted, calculate the URL-safe Base64 SHA-256 package checksum:

```bash
openssl dgst -sha256 -binary app-release.apk \
  | openssl base64 -A \
  | tr '+/' '-_' \
  | tr -d '='
```

The checksum is tied to the exact APK bytes. Rebuilding or modifying the APK requires generating a new checksum and QR payload.

## QR payload template

Replace both placeholders before generating the QR code:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.pulse.bluetoothdisable/.admin.AppDeviceAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://<PUBLIC_HOST>/bluetooth-disable/app-release.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "<URL_SAFE_BASE64_SHA256>"
}
```

The APK URL must be reachable from Android Setup Wizard over HTTPS.

## Enrollment flow

1. Build and sign the release APK.
2. Publish the **same APK bytes** at the public HTTPS URL from the payload.
3. Calculate the package checksum.
4. Generate the QR code from the completed JSON payload.
5. Factory-reset the test device.
6. Start the Android QR provisioning flow from Setup Wizard.
7. Connect the device to the internet when requested.
8. Scan the QR code.
9. Setup Wizard downloads and verifies the APK.
10. Android installs the DPC and assigns it as Device Owner.
11. The app returns fully managed mode during `GET_PROVISIONING_MODE`.
12. Provisioning completes only when `ADMIN_POLICY_COMPLIANCE` confirms Device Owner status.
13. Open the application and test Bluetooth protection.

## Important

Android 13+ expects internet connectivity during company-owned provisioning by default. We intentionally do not enable offline provisioning because the DPC itself is downloaded from a public HTTPS endpoint.

Do not publish a QR code until the release signing key and stable APK URL are finalized. Changing the APK changes the package checksum.
