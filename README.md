<p align="center">
  <img src="docs/images/icon.png" width="120" alt="OpenFlux">
</p>

<h1 align="center">OpenFluxAndroid</h1>

<p align="center">
  The Android client of OpenFlux, a network stack research tool.<br>
  An IPv4 TCP/UDP tunnel over pluggable transports, with end-to-end encryption, failover and your own exit nodes.
</p>

<p align="center">
  <a href="../../releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/OpenFluxTeam/OpenFluxAndroid?display_name=tag&color=4F7CFF"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/license-GPL--3.0--or--later-blue"></a>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84">
</p>

<p align="center">
  <b>English</b> · <a href="README.ru.md">Русский</a>
</p>

<p align="center">
  <a href="https://github.com/OpenFluxTeam/OpenFlux">Core</a> · <a href="https://github.com/OpenFluxTeam/OpenFluxDesktop">Desktop</a> · <a href="https://github.com/OpenFluxTeam/OpenFluxAndroid">Android</a> · <a href="https://testflight.apple.com/join/BwnAcdus">iOS (TestFlight; the App Store release is coming)</a>
</p>

## What it is

[OpenFlux](https://github.com/OpenFluxTeam/OpenFlux) is a network stack research tool: an IPv4 TCP/UDP tunnel with pluggable transports (including a signed JS scripting engine for writing new ones), a batched + zstd codec, and two exit-node backends (L3 raw forward / L4 gVisor proxy).

OpenFluxAndroid is its Android client. The core is compiled into the APK through the gomobile bridge (`mobile/`) and runs inside the app's VPN service; the app manages profiles, transports, keys and exit nodes.

```
apps ──> VpnService TUN  or  SOCKS5 ──> OpenFlux core ──> session of transports ──> exit node ──> Internet
                                        (in the APK)      priorities, failover       --mode l4 | l3
```

## Capabilities

### Tunnel

- **System VPN** through Android's `VpnService`: the whole device goes through the tunnel; the app's own traffic is kept out of it.
- **Proxy mode**: a local SOCKS5 proxy for the apps you point at it.
- **IPv4 TCP and UDP**; through an L3 exit, whole IP packets including ICMP.
- **Quick-settings tile** to connect and disconnect without opening the app.

### Transports and sessions

- **Native transports** of the core: Yandex Docs, Yandex Volga, Yandex Board, Mail.ru, cups.online, Telemost, Avito, MAX/OneMe, and `direct` — a plain TCP connection to your node.
- **JS transports**: signed packages for the core's scripting engine, installed from the bundle, from GitHub or by a link. The core checks each package's signature; the app shows its parameters and sends the package to your nodes.
- **Multi-transport sessions**: a profile holds several transports, each with a priority and the networks it may use (mobile data, Wi-Fi). Negotiation is authenticated; when the active carrier stalls, the session moves to the next one without tearing the tunnel down.
- **Direct first**: while `direct` to the node is alive it carries the traffic ahead of the document carriers, with a 16000-byte tunnel MTU, pooled buffers and Reno congestion control on its streams.
- **Stripe groups**: several carriers of one or mixed types work as one stripe, one lane each.
- **Codec** per profile: batched + zstd, or the legacy per-packet LZ4 codec for old nodes.

### Encryption

- **AES-256-GCM end to end** between the phone and the exit node, with a key per channel. With a key, the service that carries the documents sees only ciphertext.
- The wizard generates node keys; they travel inside the profile link and are not sent anywhere else.

### Exit nodes

- **Node wizard over SSH**, right from the phone: installs the core's exit node on a server (Linux amd64, arm64, armv7) and sets its backend and transports:
  - **L4** — a userspace gVisor TCP/IP stack that terminates connections and opens new ones; needs no root and runs anywhere;
  - **L3** — raw forwarding of IP packets with SNAT/DNAT, one connection end to end; needs root and iptables.
- **Verification**: after setup the app connects through the new node and checks that the exit address is the server's.
- **A server's channels**: list them, change their transports (the key, port and context stay), share or delete them, add new ones.
- **No server at all**: the exit can be a small PHP program on ordinary web hosting.
- **Self-updating nodes**: a node fetches new releases, checks them against `SHA256SUMS`, and rolls back if the new build does not start.
- **This phone as an exit**: other devices can connect through it.

### Also

- **`openflux://` links and QR codes** carry a whole profile: transports, priorities, key, codec; the camera scans them in.
- **Built-in browser** (WebView) for the checks some services ask for, such as SmartCaptcha or a login wall.
- **Core log** with debug levels, readable in the app.
- **Updates** from GitHub releases, checked against the release's `SHA256SUMS.txt` and installed only when you agree; the APKs are signed with one release key.
- **Optional account** at openflux.dev for subscriptions; everything above works without one.

<!-- team:start -->
## What is new in the current release

The release **3.9** carries the OpenFlux core 0.14.0 (exit node `node-v1.7.0`), built into the APK.

- **Direct first**: a plain TCP connection to the node is used ahead of the document carriers while it is alive.
- **Speed**: a 16000-byte tunnel MTU on direct with a fallback on ICMP "fragmentation needed", pooled send buffers, gated zstd (never over TLS on direct), Reno on direct streams.
- **Stripe groups** in the core instead of the old bonding.
- **A restarted node is taken over at once**: a restarted exit answers a client that is mid-session.

The full list is in [CHANGELOG.md](CHANGELOG.md).
<!-- team:end -->

## Install

Download an APK from the [releases page](../../releases/latest): the universal one, or the one for your processor (`arm64-v8a` for most phones, `armeabi-v7a` for older ones, `x86` and `x86_64` for emulators). Allow installing from the browser or file manager you used.

## Build from source

Needs JDK 17, Go, the Android SDK with NDK 27, and `gomobile` (`go install golang.org/x/mobile/cmd/gomobile@latest`).

```bash
git clone --recurse-submodules https://github.com/OpenFluxTeam/OpenFluxAndroid.git
cd OpenFluxAndroid
scripts/build-android-core.sh          # builds androidApp/libs/openflux.aar from the OpenFlux/ submodule
./gradlew :androidApp:assembleDebug    # the APK, split per ABI
```

Already cloned without `--recurse-submodules`? Run `git submodule update --init --recursive`. `scripts/build-android-core.sh ../OpenFlux` builds against a separate checkout instead of the submodule. Rebuild the AAR whenever the core's Go API or the `OpenFlux/` submodule changes.

## Structure

```
androidApp/   VPN service and the core bridge, quick-settings tile, the WebView check flow, the camera (QR)
shared/       Submodule: models, sessions, transports' settings, node wizard, screens
OpenFlux/     Submodule: the core (CLI and the mobile/ gomobile bridge)
scripts/      build-android-core.sh
```

The desktop client is [OpenFluxDesktop](https://github.com/OpenFluxTeam/OpenFluxDesktop); both use the module [OpenFluxClientShared](https://github.com/OpenFluxTeam/OpenFluxClientShared).

## Credits

- **[meepo161](https://github.com/meepo161)** — author of [OpenFluxClient](https://github.com/meepo161/OpenFluxClient), the source of this app's logic (`androidApp/`, `shared/`): multi-transport sessions with failover, encryption, the in-app check flow, the node wizard. Thank you!
- **[p1neappleXpress](https://github.com/p1neappleXpress)** — author of the [OpenFlux](https://github.com/p1neappleXpress/OpenFlux) core this app embeds: the tunnel, the transports and the negotiation protocol.

The previous, simpler single-transport app that lived in this repository is kept at the [`legacy-native-app`](../../tree/legacy-native-app) tag.

## License

GNU General Public License v3.0 or later — see [LICENSE](LICENSE).
