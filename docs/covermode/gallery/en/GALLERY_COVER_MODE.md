# Gallery Cover Mode

## Scope

Gallery Cover Mode is a local disguise that presents an offline photo gallery while the real application remains behind a hidden access gesture. Gallery data is intentionally device-local and is not exported, shared, backed up, or transferred to another device.

## Storage and privacy

- Photos are selected through Android Photo Picker. The app requests no broad photo-library permission.
- Selected images are decoded and re-encoded before storage. Source EXIF/XMP/IPTC/MakerNote/GPS/identifying metadata is not copied into the sanitized local image.
- The encrypted Gallery index may retain dimensions, color-space name, capture time, and a restricted set of non-identifying shooting parameters.
- Images, thumbnails, index, and albums are encrypted with AES-256-GCM using a non-exportable Android Keystore key and purpose-specific AAD.
- Gallery files live in `noBackupFilesDir`. Android backup and device-transfer rules exclude all app data, and `DeviceTransferGuard` removes copied private data when its device-local Keystore marker cannot be validated.

## Crash consistency

Gallery metadata is committed with `AtomicFile`. Image edits use copy-on-write revisions: new encrypted image and thumbnail files are written before the index points to them, and the previous revision is deleted only after the new index commit succeeds. Import failures remove files created by the failed transaction. Unreferenced encrypted image/thumbnail files are reconciled after a successfully decoded committed index is loaded.

## Hidden access

- A secret local image is selected during setup.
- The access sequence is exactly three different zones chosen from five zones: four corners plus center.
- Zone order matters, producing 60 valid sequences.
- The sequence is entered twice during setup.
- The plaintext sequence is not persisted. `GalleryAccessManager` stores the secret local image ID and an HMAC-SHA256 verifier backed by Android Keystore.
- Verification uses constant-time comparison.
- The sequence detector resets on timeout, repeated zone, image change, taps outside a secret zone, meaningful drag/swipe, multi-touch/zoom, viewer navigation, recovery interaction, and viewer toolbar actions.
- A pending asynchronous verification is invalidated when the sequence is reset or the viewer state changes.

### Security limitation

The three-distinct-zone design has only 60 combinations and currently has no persistent rate limit or lockout. The HMAC protects the stored verifier but does not increase the entropy of the user gesture. This is an intentional compatibility constraint of the current Gallery v1 interaction and should not be treated as a high-entropy authentication factor. Strengthening the gesture space or adding an attempt policy is a future product/security decision because either change affects the established access semantics.

## Recovery

Gallery uses the shared Cover Mode emergency recovery flow: a continuous three-second hold on the Gallery title, Android system authentication, and a separate explicit reset confirmation. Recovery returns to DEFAULT launcher state and removes the temporary Gallery access rule/key while preserving the encrypted local Gallery data.

## Lifecycle and Cover Mode switching

Switching away from Gallery clears the temporary hidden-access verifier/key but preserves Gallery photos, albums, and favorites. Re-entering Gallery requires a new secret image/sequence setup. Deleting the current secret image invalidates only the access rule; Gallery data and the active disguise remain available for emergency recovery.

## Known validation boundaries

Automated tests cover storage, sanitization, access persistence, Cover Mode transitions, backup-permission boundaries, atomic-index recovery, copy-on-write edits, and orphan reconciliation. Physical-device behavior, OEM Photo Picker implementations, very large-image memory pressure, and visual behavior at extreme font/display scaling remain manual validation areas.
