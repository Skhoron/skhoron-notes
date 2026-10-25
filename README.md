Skhoron Notes

Notes, checklists, attachments. Everything stays on the device, with no network and no account. Part of
Skhoron — the same privacy logic as in VPN, Pass, Browser.

Not yet on Google Play. The APK is available either through Actions or by building it yourself in Android Studio.

Features

- Markdown text or a checklist, switchable directly in the editor
- Attachments of any file type, not just photos
- List, grid, or free layout — a note can be dragged anywhere on the canvas
- Folders of any nesting depth
- Sign-in using a PIN, password, or pattern lock. No biometrics — it is someone else's closed
  mechanism, not something a person controls themselves
- Your own logo can be loaded directly in the app, without rebuilding
- Save format: .md, .txt, .json, .yml, or your own extension

Security

The database is encrypted using SQLCipher. The key is random and not tied to the PIN — even
if a person skips sign-in protection on the first launch, the data is still encrypted.

The PIN, password, and pattern lock are stored as PBKDF2 hashes. There is an attempt limit, followed by a temporary
lockout. The screen locks again every time the app returns from the background. FLAG_SECURE ensures content
does not appear in screenshots or in the recent apps preview.

There is no internet permission in the manifest. This is a build-level decision, not a toggle in the settings.

The code is open under Apache 2.0. Check it yourself instead of taking our word for it. Found a vulnerability? Report it in Issues.

Architecture

Two modules.

"core" — Room, repository, DataStore, crypto layer. No UI.

"app" — Compose on top of core. No direct access to Room or SQLCipher; all this
work is handled in "core/.../DatabaseFactory.kt".

This does not make it cross-platform — "core" still depends on "Context". But the logic can
be reused in another Skhoron product.

Stack: Kotlin, Compose (Material 3), Room + SQLCipher, DataStore, Navigation Compose,
Coil, EncryptedSharedPreferences.

Build

Actions: Actions tab → latest build → Artifacts → apk.

Locally: open the root directory in Android Studio, let it generate the wrapper, Sync, Run.

Min SDK 26.

Not Implemented

Folder tree in the UI — the data supports nesting, but there is no navigator for it.

Compression of old notes — the settings are there, but the worker has not been written.

Export through SAF — FileProvider is ready, but there is no button in the interface.

License

Apache 2.0, LICENSE file.