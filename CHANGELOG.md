# Changelog

All notable changes to OpenFluxAndroid. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [2.12.0] - 2026-10-11 (meepo161/OpenFluxAndroid)

### Added

- Accounts and subscriptions. People who run no node of their own sign in and
  press "Подключить": the owner's service (`opensub`, core 0.6.0,
  docs/subscriptions.ru.md) gives them locations built from the owner's nodes,
  without documents, keys or SSH. Account with a login and password (no
  email), a recovery key shown once, a trial, a plan chosen in the app and paid
  by the owner's own means (the owner confirms the order and the app notices
  it by itself), devices per plan, password change, sign-out, account deletion.
  A subscription key (`openflux://sub?…`, from a link, QR, file or the paste
  field) works without an account. The service is pinned by its certificate's
  SHA-256 carried in the key.
- "Подключение" (the Подписка tab before signing in), after Amnezia's screen:
  one paste field for a key or link, and the other ways to connect under it
  (an account, a node on your own server, a file, a QR code, a node without a
  server, by hand, restore access). Home shows the same two buttons until there
  is a connection, and a strip with the subscription's end date after.
- Change a node from its profile: "Изменить транспорты на сервере…" in the
  profile's details and in its settings opens the node wizard on that
  profile's channel (found on the server by its port); only the server's
  password is asked. The key, port and links stay, the profile is updated.
- The app updates itself from its GitHub releases: a few seconds after the
  start and every six hours it looks for a newer release (switch in
  Settings → О программе) and offers it in a banner with "Обновить",
  "Пропустить" and "Позже". Nothing is downloaded before the tap; the
  installer is checked against the release's `SHA256SUMS.txt` and refused
  without a matching sum; only GitHub's hosts over https are used.
- Android installs through the system installer (the person confirms; "Install
  unknown apps" is allowed for OpenFlux first), picks the APK of the phone's
  CPU, and reports why when the system refuses (a different signing key,
  cancelled, no space).

### Changed

- "Логи" is no longer a tab: the journal opens from Settings → Интерфейс and
  from Home ("Журнал"). Fewer tabs, the same screen.
- "Позже" on a Yandex check now keeps further Yandex checks quiet for ten
  minutes (the other transports carry on) instead of asking again at once.
- Forms clear their error as soon as a field is edited; the recovery key is
  shown four groups to a line.
- The tab bar hides while the keyboard is up.

### Removed

- The "Ночные сборки" switch and the link to a stale Android repository in
  About: this fork publishes one channel.

## [2.11.1] - 2026-10-10 (meepo161/OpenFluxAndroid)

### Fixed

- Node wizard: after changing the documents of a channel already on the
  server, the done step names that channel and its port (it showed the new
  channel the wizard had prepared, port 0).

## [2.11.0] - 2026-10-10 (meepo161/OpenFluxAndroid)

### Added

- Node wizard: change the documents of a channel already on the server. The
  transports step lists the server's channels; "Изменить транспорты" reads one
  and "Применить" adds or removes documents in place. The key, port and
  encryption context stay, so links handed out keep working. With the
  channel's profile on this device (made by the wizard or imported from its
  link) the wizard builds the new link with the channel's context, checks the
  channel through it and updates the same profile.
- From p1neappleXpress 2.3.x: script (JS) transports behind "Экспериментальные
  функции", the "без сервера" PHP-hosting wizard, nightly release channel.

### Changed

- Core 0.5.1 (meepo161/OpenFlux c36eb0c) with p1neappleXpress 0.4.2 merged in;
  the node wizard installs node-v1.4.1.
- Integrated current upstream/main with fork and bonding; retained document
  creation, Yandex sign-in, accounts (a fresh sign-in goes on to your own node)
  and developer mode from the fork.

### Fixed

- Mail.ru Docs carrier joins documents again (the server refused a Chrome user
  agent with 409).
- cups.online: a lost message no longer stalls the stream.
- The Accounts tab follows developer mode again next to the Transports tab of
  the experimental features.

## [2.10.0] - 2026-09-30 (meepo161/OpenFluxAndroid)

### Added

- Бондинг: переключатель в профиле Session. Все транспорты работают
  одновременно: скорости складываются, а если сеть или документ пропадёт,
  остальные продолжают. Соединения распределяются по транспортам, UDP (SRT)
  делится по пакетам. Нужна нода node-v1.3.0 или новее; со старыми нодами
  профиль работает как раньше. Три документа Mail.ru, три соединения:
  0,84 МБ/с против 0,59 на одном.
- Сеть для каждого транспорта: любая, мобильная, Wi-Fi. Приложение держит мобильные данные включёнными рядом с Wi-Fi
  (разрешение CHANGE_NETWORK_STATE) и привязывает к сети сокеты транспорта.
- Главная показывает транспорты бондинга с их сетями.
- Bumps `OpenFlux` to 563ee0c (node-v1.3.0), `shared` to 204a159.

## [2.9.4] - 2026-09-30 (meepo161/OpenFluxAndroid)

### Fixed

- A node's profile (made by «Своя нода») started in exit mode ran a second
  exit on the node's own document: both exits took and answered the
  clients' packets and an iOS client got nowhere. The app now refuses to
  run it as an exit and says to switch to «Клиент» or make a separate
  profile with its own document.
- The wizard's check connects to the new node as a client whatever mode
  the app is in; it no longer asks to switch the mode first.
- Bumps `shared` to 9c5fc2f.

## [2.9.3] - 2026-09-30 (meepo161/OpenFluxAndroid)

### Fixed

- Mail.ru carrier: with both ends on the document, the connection to it
  dropped every half minute (the editor server locked the document on one
  end and dropped it when it did not unlock it), and each rejoin stalled
  traffic for up to 30 seconds; long uploads, such as a stream, broke. The
  carrier now unlocks the document for the other end. The wizard installs
  node core node-v1.2.3.
- Bumps `OpenFlux` to 14dc27d.

## [2.9.2] - 2026-09-30 (meepo161/OpenFluxAndroid)

### Fixed

- A node the wizard sets up now also serves clients that speak only the
  classic protocol, such as the iOS app: they were dropped on every carrier
  of the node and never got through. The wizard installs node core
  node-v1.2.2; nodes with the core updater on get it by themselves.
- The core no longer drops packets when a carrier's batch queue is full
  (node-v1.2.1 backpressure): it waits and retries in order.
- Bumps `OpenFlux` to d420317.

## [2.9.1] - 2026-09-29 (meepo161/OpenFluxAndroid)

### Fixed

- «Своя нода», step 4 (verification) on an Android phone in VPN mode: the app
  is kept out of its own VPN, so it could never ask where the traffic
  leaves; the step retried until its 150 s timeout and failed. It now counts
  the new node's Session answer (right key and document) as the proof, and
  Home says the exit address is seen in a browser (api.ipify.org).
- «Проверить ещё раз» after a failed verification ended at once with the old
  error: it read the previous attempt's state before the new connection
  started. It now waits for the new connection.
- A Yandex board is joined as a guest: the Yandex sign-in is no longer put
  into its carrier or offered to its node.
- Bumps `shared` to 48f1255.

## [2.9.0] - 2026-09-29 (meepo161/OpenFluxAndroid)

### Added

- «Своя нода» makes every document right in the wizard, no developer mode:
  the Yandex document (Volga), a Mail.ru document (in Cloud, published and
  open to editing by link) and a Yandex board (open to guests as editors).
  The sign-in opens in the Accounts window; a saved one skips it. A pasted
  link still works for each.
- The wizard offers two more carriers: Yandex Docs (the same Yandex document
  through the older transport) and Yandex Board. The node takes them too:
  `node-install.sh` writes `yandex` and `boards` transports, pinned at
  [`6d277eb`](https://github.com/meepo161/OpenFlux/commit/6d277ebd05ef69348b8f12ba8915c90fdcfbb19f).
- The profile editor makes a board for a Yandex Board carrier.
- Bumps `OpenFlux` to `7006309` and `shared` to `de4a638`.

## [2.8.0] - 2026-09-29 (meepo161/OpenFluxAndroid)

The fork on p1neappleXpress 2.1.0: links read and made by the core, a
classic profile as the exit node, one protocol for every client.

### Added

- Developer mode: ten taps on the app version in Settings → About show the
  «Аккаунты» tab (a switch there hides it again). In developer mode the node
  wizard also offers to sign in to Yandex and make the document, in the
  Accounts tab's sign-in window; otherwise it takes your own link.

### Fixed

- The node wizard's sign-in menus («другой способ получить код») no longer
  close as the keyboard opens: the page opens full screen in the Accounts
  tab's sign-in window instead of inside the wizard's scrolling step.

### Changed

- Bumps `OpenFlux` to the fork's [`57c69d7`](https://github.com/meepo161/OpenFlux/commit/57c69d706a71cd77f1951efeb8b438d275e1df86)
  (`v0.3.0`, node core `node-v1.2.0`) and `shared` to
  [`806f0b4`](https://github.com/meepo161/OpenFluxClientShared/commit/806f0b48de9dee598e91810f11a756f56ca578e9).

## [2.7.0] - 2026-09-28 (meepo161/OpenFluxAndroid)

### Added

- «Аккаунты»: sign in once to Yandex or Mail.ru in the built-in browser; the
  app keeps only the session (owner-only `accounts.json`, never in backups,
  logs, QR codes or links) and shows each service's status — signed in (with
  the login and when it was last checked), expired, or asking for a check.
  Sessions are rechecked every 30 minutes without the browser; «Войти заново»
  is one button, on the card and in a banner on Home when a profile needs it.
- «Создать документ» makes the channel's document with the saved account, no
  `cookies.txt` export: Yandex — a document on Disk with editing by link;
  Mail.ru — a document in Cloud (`/openflux`), published and switched to
  editing by link, then checked the way the core opens it (anonymous
  `r7/edit`). The same button sits under a document field in the profile
  editor; the node wizard uses the saved Yandex account too.
- The Yandex sign-in goes into the core's cookie store before it connects, and
  to your own node (from the wizard) over the tunnel, again after a fresh
  sign-in. Someone else's node gets it only by hand, after a warning. The
  Mail.ru account is never put into the core: its transport opens the
  document anonymously and Cloud refuses that request with the account's
  cookies.
- Cups.online: «Сгенерировать комнаты» in the profile editor opens four rooms
  without a node; the node given the same string joins them.

### Fixed

- Creating a Yandex document failed with «Failed to fetch»: Disk now
  redirects `/editnew` to `docs.yandex.ru`, where the browser refused a
  readable (CORS) request. Affected the node wizard as well.
- The built-in browser pages saw a 0 px viewport (the WebView was sized to
  its content): VK ID for Mail.ru stayed blank, and Yandex's «Resend code»
  sheet (SMS, Telegram) closed as soon as it opened. The sign-in now takes
  the whole screen on a phone.
- The core library is linked for 16 KB pages; Android 15+ no longer runs the
  app in a compatibility mode with a warning on every start.

## [2.6.0] - 2026-09-28 (meepo161/OpenFluxAndroid)

The first release of the meepo161 fork: the app, its update check, the core
submodule and the node wizard's core come from the fork's repositories
(meepo161/OpenFlux, meepo161/OpenFluxClientShared, meepo161/OpenFluxAndroid).

### Added

- «Своя нода»: step 1 asks whose core the server gets — the fork's
  (`meepo161/OpenFlux`, default) or the original (`p1neappleXpress/OpenFlux`);
  the node's auto-update then follows that repository.
- The core is built into the APK (fork `v0.2.0`); the choice of the
  node's core is in the wizard.

### From p1neappleXpress/OpenFluxAndroid (not yet released there)

### Added

- Developer mode (ten taps on the app version in Settings → About) shows
  the «Аккаунты» tab: sign in once to Yandex or Mail.ru in a WebView; the
  app keeps only the session (owner-only `accounts.json`, never in backups,
  logs, QR codes or links) and shows each service's status — signed in,
  expired, or asking for a check. Sessions are rechecked every 30 minutes;
  «Войти заново» is one button.
- «Создать документ» makes the channel's document with the saved account:
  Yandex — a document on Disk with editing by link; Mail.ru — a document in
  Cloud (`/openflux`), published and switched to editing by link. In
  developer mode the node wizard offers it too, in the same sign-in window.
- The Yandex sign-in goes into the core's cookie store before it connects,
  and to your own node (from the wizard) over the tunnel. The Mail.ru account
  is never put into the core: its transport opens the document anonymously.
- Cups.online: «Сгенерировать комнаты» in the profile editor opens four
  rooms without a node.
## [2.3.2] - 2026-10-07

Hotfix: the core 0.4.2.

### Fixed

- **Mail.ru Docs: a link that only lets the document be read no longer drops the connection every few
  seconds.** The transport sent the editor's `saveChanges` now and then, and for a link without edit rights
  (what most public links are) the server closed the connection on every one, so the channel was up for
  moments between reconnects. It is not sent there any more (core 0.4.2, `OpenFlux` bumped to
  [`74cac6d`](https://github.com/p1neappleXpress/OpenFlux/commit/74cac6d)). The Engine.IO / Socket.IO
  handshake is also completed before the auth is sent (thanks to Novarg93,
  [OpenFlux#147](https://github.com/p1neappleXpress/OpenFlux/pull/147)). Exit nodes on a VDS need the same
  core: «Своя нода» installs `node-v1.2.2`.

## [2.3.1] - 2026-10-07

Hotfix: the core 0.4.1.

### Fixed

- **Yandex Docs / Boards / Volga through a node: the check no longer repeats forever.** The check the
  node asks for is passed in the app's browser from the node's IPv4 address (through the tunnel), but
  the node's own requests went out over IPv6 on hosts that have an IPv6 route, so Yandex never accepted
  the cookies the app handed over. The core now dials Yandex IPv4 first (core 0.4.1, `OpenFlux` bumped to
  [`d0578b6`](https://github.com/p1neappleXpress/OpenFlux/commit/d0578b6)). Exit nodes on a VDS need the
  same core: «Своя нода» installs `node-v1.2.1`.
- **Exit node, L3: the log (`-d`) shows the tunnel's packets only**, not every packet the host receives.

## [2.3.0] - 2026-10-07

### Added

- **«Экспериментальные функции»** (Settings): the JS-transport features below are off by default and
  for settings saved before; nothing of them is shown, started or fetched, and the JS engine is not
  started when the app starts, until the switch is on.
- **JS transports** (experimental, off by default): the «Транспорты» tab, installing a signed
  transport by link, file or catalog with a trust dialog and the author's key fingerprint, the
  settings a transport declares (a wizard page), its own setup and login pages in a WebView, update
  check with changelog, apply and roll back, and the transports shipped in the app (installed, or
  replaced by a newer shipped version, the first time the switch is turned on). A profile that uses
  a JS transport does not connect while the switch is off, and says so.
- A second release channel: nightly test builds next to the main releases. A
  nightly is a GitHub prerelease tagged `nightly-<date>-<commit>`, built by the
  new `Nightly` workflow from the `nightly` branch (on a push to it, and every
  night when it has a commit without a build), signed with the same key as a
  main release so it installs over one; the newest 7 are kept. Settings → About
  has a «Ночные сборки» switch: with it on, «Проверить обновления» also looks
  at nightlies; the main channel never sees them.

### Changed

- Bumps `OpenFlux` to the core 0.4.0 ([`a415c09`](https://github.com/p1neappleXpress/OpenFlux/commit/a415c09d1d1006a201b5e9523d2f30d7ee2b4149)) and `shared` to
  [`a7fca72`](https://github.com/p1neappleXpress/OpenFluxClientShared/commit/a7fca72).
- The core's Yandex check on a profile without a key now reaches the app (a profile without a Session
  used to log "external solver required" and never ask for the check).

## [2.2.0] - 2026-10-01

### Fixed

- «Без сервера»: the node now stays up on any host and keeps the tunnel going
  across its generations, and the install no longer fails when the host cuts a
  transfer short (InfinityFree's FTP aborted the link parser part way): a file is
  sent again, and `auto` goes on in plain FTP after it keeps failing over TLS.
  The wizard also adds a node already on the hosting from its site and access key
  (no FTP), takes a key of your own on an FTP install, opens the node's panel in
  the browser, and shows which generation serves («работает · поколение N»).
  Bumps `OpenFlux` to
  [`bb55dc3`](https://github.com/p1neappleXpress/OpenFlux/commit/bb55dc3) and
  `shared` to
  [`98572e2`](https://github.com/p1neappleXpress/OpenFluxClientShared/commit/98572e2).
- The core's Yandex Boards moves packets as `modify-objects` text objects (the
  board no longer relays `notify-position`), and Yandex Docs and Mail.ru Docs
  send the web client's `saveChanges` stream built from the live session.

### Added

- With carriers at equal top priority the badge and «Сейчас через» name all of
  them ("Board + Volga 2"), as the core spreads traffic over the whole group.

- «Без сервера»: a new profile mode where the exit is a small PHP program on an
  ordinary web hosting (free or paid, any with PHP and FTP/FTPS) instead of your
  own server. The wizard (Профили → «Без сервера») takes the FTP data and the
  site's address, creates a cups.online room (or takes a Mail.ru document),
  uploads the node, checks that the site runs it, starts it and connects
  through it before saving the profile. Works as the system-wide VPN or as a
  local SOCKS5 proxy; no key to keep, TCP on ports 80 and 443 only. The node
  renews itself while it is used, and connecting a profile made this way first
  asks its node to run when the hosting is reachable. Its `openflux://` link and
  QR carry the mode (never the node's token).

- «Своя нода»: a new channel is no longer Yandex-only. Step 2 picks any mix
  of a Yandex document (your own link), a Mail.ru public document and
  cups.online rooms (created automatically), with direct always on as the
  backup; the link and the saved profile carry all of them.
- «Автообновление ядра» on the plan step (on by default): the server's
  `openflux-node-update.timer` checks the newest `node-v*` release every
  6 hours, verifies it against the release's `node-install.sh` and
  `SHA256SUMS`, restarts the channels and rolls back if one does not stay
  up.
- Bumps `OpenFlux` to [`ee7cf56`](https://github.com/p1neappleXpress/OpenFlux/commit/ee7cf56d27549018d5fc3f6a5a31445fff55380c)
  and `shared` to [`b23354a`](https://github.com/p1neappleXpress/OpenFluxClientShared/commit/b23354ab2ecf8968eca8a3d1b7b66f6f74be07aa).

## [2.1.0] - 2026-09-28

### Changed

- Share links are read and made by the core, the way every client does:
  bumps `OpenFlux` to [`2ec01a5`](https://github.com/p1neappleXpress/OpenFlux/commit/2ec01a5) (core 0.2.0) and `shared` to
  [`ae5e59a`](https://github.com/p1neappleXpress/OpenFluxClientShared/commit/ae5e59a).
  - A link that picked up line breaks, spaces, non-breaking or zero-width
    characters, padding or the standard base64 alphabet on the way imports,
    as on iOS, instead of «Ссылка повреждена».
  - The same profile makes the same link on Desktop, Android and iOS; the
    core names the encryption context of a Session link, the app no longer
    derives it.
  - A refused link says why: not a link, cut short, letters changed case,
    unknown transport, key too short, and so on.
  - The node wizard installs `node-v1.1.0`, the node build of core 0.2.0.
- The node wizard's document step takes the link of a document you
  created; the button that signed in to Yandex and created one is gone.
- The release notes show this changelog.

### Fixed

- Clients and nodes built from different trees now connect: bumps `OpenFlux`
  to [`f8f3476`](https://github.com/p1neappleXpress/OpenFlux/commit/f8f34767a5732febd5965ac6cf95bad51b70cf99).
  - A classic profile with a key runs the Session and falls back to the
    classic layering for a classic or older node, on the same carrier; a
    node set up as classic serves both kinds of client. Nodes set up for
    the Session stay Session-only.
  - The encryption context follows one rule everywhere, and a client whose
    context differs from the node's finds the node's instead of timing out
    (classic cups.online was the common case).
  - The classic codec (batched or legacy) is no longer a hard requirement:
    both are accepted and the client switches when the node does not answer.
  - boards no longer drops the connection every 20 seconds; yandex and
    mailru reconnect when their socket dies.
  - The log explains a failed handshake: wrong key, a node in the other
    mode, the codec or context picked, a connection taken over by another
    client.

## [2.0.1] - 2026-09-27

### Fixed

- An exit node deployed by the node wizard never actually connected: a
  `.conf`-only `Role = exit` (every node-wizard deployment) left the core's
  internal exit/client flag stuck at its pre-config value, so the exit
  never answered the handshake and crash-looped instead. Bumps `OpenFlux`
  to [`e8f735a`](https://github.com/p1neappleXpress/OpenFlux/commit/e8f735a98c1ba9e091956416fde5c5ef92d3cd66).
- The startup log always printed `Transport: yandex` for session/multi-transport
  profiles regardless of which transports were actually configured (a stale
  flag default, not a functional bug — the correct transports ran either
  way). Now prints the actual list, e.g. `Transport: boards, direct (session)`.
- Cups.online profiles with no room codes couldn't be saved or connected;
  the node generates its own rooms, so an empty value is valid for this
  transport only.
- An exit node always listened for Direct (TCP on `0.0.0.0:<port>`) and put
  it into the clients' link, even when the profile had no Direct transport.
  It now listens only when the profile has Direct, at that transport's
  priority ([#11](https://github.com/p1neappleXpress/OpenFluxAndroid/pull/11)).
- A phone exit on cups.online without room codes put cups.online into its
  link with no rooms, so clients could not join; the link now carries the
  rooms the node created ([OpenFlux#123](https://github.com/p1neappleXpress/OpenFlux/pull/123)).

### Added

- Settings → Ядро OpenFlux: a core log-level picker (Выкл / -d / -dd /
  -ddd, the core's `--debug=N`) in place of the "Подробный журнал ядра"
  switch. The embedded core used to run at -dd on every connect whatever
  the switch said — formatting a log line for every packet — and the
  switch only hid those lines from the log view; the level now reaches the
  core itself ([OpenFlux#121](https://github.com/p1neappleXpress/OpenFlux/pull/121)).
  The default is Выкл: choose -dd to see transport errors, handshakes and
  the encryption (KDF) context.
- Node-wizard deployment logging: every SSH/RPC call and the wizard's own
  step narration now goes to the Logs tab, so a stuck deployment is
  diagnosable without a debugger.
- Home → Подключение: a "Сейчас через" row for profiles with several
  transports, and carriers named as in the app ("Board 2", not `boards-2`)
  there and in the "через …" badge.

## [2.0.0] - 2026-09-27

First release of this app. Replaces the previous single-transport native
app (tun2socks + pdnsd JNI, preserved at the
[`legacy-native-app`](../../tree/legacy-native-app) tag) with the Compose
Multiplatform app originally built by [@meepo161](https://github.com/meepo161)
in [OpenFluxClient](https://github.com/meepo161/OpenFluxClient), moved here
with his agreement.

### Added

- System VPN or local SOCKS5.
- Multi-transport sessions with automatic failover and priority-based
  switching (including `direct`).
- AES-256-GCM session encryption.
- SmartCaptcha and login handling in a built-in browser, including a
  transport check on the exit node, passed through the tunnel from its
  own address.
- A node-deployment wizard: install an exit node on your own VPS over SSH
  from the app.
- `shared/` and `OpenFlux/` as git submodules ([OpenFluxClientShared](https://github.com/p1neappleXpress/OpenFluxClientShared)
  and the [OpenFlux](https://github.com/p1neappleXpress/OpenFlux) core), so
  this app always builds against one pinned, single copy of each instead of
  a vendored one.
- `.github/workflows/release.yml`: a `v*` tag builds and signs a release
  APK and publishes it here.

### Credits

- [@meepo161](https://github.com/meepo161) — this app's UI and logic.
- [@p1neappleXpress](https://github.com/p1neappleXpress) — the OpenFlux
  core it embeds.
- [@damnurmum](https://github.com/damnurmum) — `openflux://` links/QR codes
  and the cups.online transport in the core, which this app's share and
  scan screens build on.
