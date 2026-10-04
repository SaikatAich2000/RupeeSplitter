# Privacy

Rupee Splitter is built to be private by default. This page states exactly what the
app does — and does not — do.

## Short version

- No internet permission.
- No networking code of any kind.
- No accounts, no sign-in.
- No analytics, telemetry, crash reporting or advertising.
- No cloud sync and no cloud backup.
- Nothing is sent automatically; sharing is only initiated by you.

## Permissions

The app declares **zero** permissions. In particular it does **not** request
`android.permission.INTERNET`, and the source contains no networking client.

```xml
<!-- app/src/main/AndroidManifest.xml -->
<!-- No <uses-permission> elements at all. -->
```

## Where your data lives

Nowhere permanent. The amount you type is held in memory only. It is never written to
disk, never stored in a database and never included in Android backups:

- `android:allowBackup="false"`
- `res/xml/backup_rules.xml` → exclude everything (Android 11 and lower)
- `res/xml/data_extraction_rules.xml` → exclude everything for cloud backup and
  device transfer (Android 12+)

On rotation, Android may keep the current amount in the private saved-instance bundle
so the screen is not reset. That bundle lives only for the current session.

## Copy and Share

- **Copy** uses Android's own `ClipboardManager` — the text goes to the system
  clipboard you already control.
- **Share** opens Android's built-in share sheet — you pick the destination.

Neither action gives Rupee Splitter network access.

## Verify the claim yourself

1. Open `app/src/main/AndroidManifest.xml` — there is no `<uses-permission>` element.
2. Search the project for `INTERNET`, `http://`, `Socket` or `Url` — no matches in app
   code.
3. Install the app, switch the phone to airplane mode, and use it. Everything works.
