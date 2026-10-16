# OpenFluxAndroid

Android client for [OpenFlux](https://github.com/p1neappleXpress/OpenFlux):
system VPN or local SOCKS5, multi-transport sessions with automatic
failover, AES-256-GCM encryption, and the in-app flow for passing a
transport's check (SmartCaptcha, a login wall) through the built-in browser.

This repository's app code and UI (the `androidApp/` and `shared/` modules)
come from [meepo161/OpenFluxClient](https://github.com/meepo161/OpenFluxClient),
used here with the author's agreement. **Huge thanks to
[@meepo161](https://github.com/meepo161)** — see [Credits](#credits) below.
The previous, simpler single-transport app that used to live in this
repository is preserved at the [`legacy-native-app`](../../tree/legacy-native-app)
tag.

## Getting the code

```bash
git clone --recurse-submodules https://github.com/p1neappleXpress/OpenFluxAndroid.git
```

Already cloned without `--recurse-submodules`?

```bash
git submodule update --init --recursive
```

This checks out two submodules:

- `shared/` → [OpenFluxClientShared](https://github.com/p1neappleXpress/OpenFluxClientShared),
  the Compose Multiplatform UI and models shared with
  [OpenFluxDesktop](https://github.com/p1neappleXpress/OpenFluxDesktop).
- `OpenFlux/` → [OpenFlux](https://github.com/p1neappleXpress/OpenFlux), the
  core this app embeds as a library (gomobile).

## Building

Needs JDK 17, Go, the Android SDK and NDK 27, and `gomobile`
(`go install golang.org/x/mobile/cmd/gomobile@latest`).

```bash
scripts/build-android-core.sh          # builds androidApp/libs/openflux.aar
                                        # from the OpenFlux/ submodule
./gradlew :androidApp:assembleDebug    # APK, split per ABI
```

`scripts/build-android-core.sh` also accepts an explicit path
(`scripts/build-android-core.sh ../OpenFlux`) if you'd rather build against a
separate checkout than the submodule.

## Structure

```
androidApp/   VPN service, WebView-based check flow, camera (QR), settings
shared/       Submodule: models, service interfaces, design system, screens
OpenFlux/     Submodule: the core (CLI + the mobile/ gomobile bridge)
scripts/      build-android-core.sh
```

## Credits

- **[meepo161](https://github.com/meepo161)** — author of
  [OpenFluxClient](https://github.com/meepo161/OpenFluxClient), the source of
  this app's UI and logic (`androidApp/`, `shared/`): multi-transport
  sessions with failover, AES-256-GCM encryption, the in-app check/captcha
  flow, the node-deployment wizard, and the Compose design system. Thank you!
- **[p1neappleXpress](https://github.com/p1neappleXpress)** — author of the
  [OpenFlux](https://github.com/p1neappleXpress/OpenFlux) core this app
  embeds: the tunnel, transports, and negotiation protocol.
- **[damnurmum](https://github.com/damnurmum)** — author of
  [OpenFlux-Android](https://github.com/damnurmum/OpenFlux-Android) and,
  in the core, `openflux://` links and QR codes, and the cups.online
  transport, which this app's share and scan screens build on. Thank you!

## License

GNU General Public License v3.0 or later — see [LICENSE](LICENSE).
