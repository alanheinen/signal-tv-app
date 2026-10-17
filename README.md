# Signal for Google TV

A small Android TV app for [Signal TV](https://github.com/alanheinen/iptv-epg)
and [Signal Radio](https://github.com/alanheinen/radio-guide): two tiles on the
home screen, each opening one of the sites full screen in its TV mode, driven
by the remote.

The app is a web view. The guide, the player and all the navigation live in
the sites themselves (`?tv=1`), so they update with the server and the app
rarely needs reinstalling. The app adds what a web page can't do on its own:

- **Back:** hands the remote's Back button to the page (`window.signalBack()`),
  and leaves when the page has nothing to go back from
- **Remote keys:** passes on channel up/down, play/pause, stop, next/previous,
  Guide and Info, which a web view otherwise keeps to itself
- **Home and back again:** pauses playback when you leave and resumes when you
  return
- **Screen on:** keeps the screen awake while you use it, until four hours pass
  without a key press
- **Offline:** a "can't reach" screen with a retry instead of a browser error

It has no dependencies beyond the Android SDK, and the APK is about 140 KB.
Android 8 (API 26) or newer.

## Build

You need your own server addresses and a signing key. Keep the key: Android
only installs an update signed with the same one.

```bash
keytool -genkeypair -keystore signal.jks -alias signal -keyalg RSA -keysize 3072 \
  -validity 36500 -dname "CN=Signal TV app"           # once; asks for a password

./gradlew assembleRelease \
  -Psignal.tvUrl=https://tv.example.com/ -Psignal.radioUrl=https://radio.example.com/ \
  -Psignal.keystore=$PWD/signal.jks -Psignal.storePassword=…
# → app/build/outputs/apk/release/app-release.apk
```

No Android SDK? [`docker/build.Dockerfile`](docker/build.Dockerfile) is one,
with adb for installing:

```bash
docker build -t signal-tv-app-build -f docker/build.Dockerfile docker
docker run --rm -v "$PWD":/src signal-tv-app-build ./gradlew assembleRelease …
```

CI ([`.github/workflows/apk.yml`](.github/workflows/apk.yml)) builds the APK on
every push to `main` from repository variables and secrets; the workflow file
says which.

## Install on the TV

1. On the TV: **Settings → System → About**, select **Android TV OS build**
   seven times to unlock developer options.
2. **Settings → System → Developer options**: turn on **USB debugging** and,
   where offered, **Network debugging** (some TVs call it ADB over network).
3. From a computer on the same network:

   ```bash
   adb connect <tv-address>:5555      # accept the prompt on the TV, tick "always allow"
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

The tiles appear under **Apps**; long-press one to move it to the home row.
`adb install -r` with a newer build updates it in place.

The TV must be able to reach and resolve the servers. Google devices sometimes
use their own DNS rather than your network's, which matters if the sites only
resolve locally.

## License

[MIT](LICENSE)
