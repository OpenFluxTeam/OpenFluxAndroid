# OpenFluxAndroid

Android client for [OpenFlux](https://github.com/OpenFluxTeam/OpenFlux):
system VPN or local SOCKS5, multi-transport sessions with automatic
failover, AES-256-GCM encryption, and the in-app flow for passing a
transport's check (SmartCaptcha, a login wall) through the built-in browser.

Current release: **3.8.0**, with the OpenFlux core 0.14.0 (node-v1.7.0) built into
the APK. Releases are on the
[releases page](https://github.com/OpenFluxTeam/OpenFluxAndroid/releases); the app
checks for them itself (Настройки → «О программе») and installs one only when
you agree.

What the current release brings:

- **Direct first.** A `direct` transport (a plain TCP connection to the node)
  is used ahead of the document carriers (Yandex, Mail.ru, cups.online, MAX…)
  while it is alive, and the document carriers stay as the reserve. A new
  `direct` transport in a profile starts at priority 110.
- **Speed.** Large packets on direct, pooled buffers, compression only where it
  pays, Reno on direct streams. The measurements and the method are in the
  core's `TRANSPORT_PERFORMANCE.md`.
- **Stripe groups** of several carriers work as one stripe. This is a core feature
  for now (`--lanes N` or `Lanes = N` on a node); the app has no screen for it.
- **A busy VPN interface no longer stops the VPN.** A full write buffer
  (ENOBUFS) is retried, and only a packet that still does not fit is dropped.
- **A restarted node is taken over at once**, not after 30 s of silence.

This repository's app code and UI (the `androidApp/` and `shared/` modules)
come from [meepo161/OpenFluxClient](https://github.com/meepo161/OpenFluxClient),
used here with the author's agreement. **Huge thanks to
[@meepo161](https://github.com/meepo161)** — see [Credits](#credits) below.
The previous, simpler single-transport app that used to live in this
repository is preserved at the [`legacy-native-app`](../../tree/legacy-native-app)
tag.

## Getting the code

```bash
git clone --recurse-submodules https://github.com/OpenFluxTeam/OpenFluxAndroid.git
```

Already cloned without `--recurse-submodules`?

```bash
git submodule update --init --recursive
```

This checks out two submodules:

- `shared/` → [OpenFluxClientShared](https://github.com/OpenFluxTeam/OpenFluxClientShared),
  the Compose Multiplatform UI and models shared with
  [OpenFluxDesktop](https://github.com/OpenFluxTeam/OpenFluxDesktop).
- `OpenFlux/` → [OpenFlux](https://github.com/OpenFluxTeam/OpenFlux), the
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
separate checkout than the submodule. Rebuild the AAR after the core's Go API
or the `OpenFlux/` submodule changes.

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
