# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`ra-vpn-mobile` is an Android VPN client (`pro.ra_tech.ra_vpn.mobile`). It implements a custom UDP/TCP VPN protocol against a server, tunneling all device traffic through an Android `VpnService`. The thin Kotlin UI layer just starts/stops the service; all of the protocol and networking is Java built on **Netty**.

## Build & test

Uses the Gradle wrapper (`./gradlew`). `local.properties` must point `sdk.dir` at an Android SDK.

```bash
./gradlew assembleDebug        # build debug APK
./gradlew installDebug         # build + install on connected device/emulator
./gradlew test                 # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest # instrumented tests (needs device/emulator)
./gradlew lint                 # Android lint
./gradlew :app:testDebugUnitTest --tests "pro.ra_tech.ra_vpn.mobile.ExampleUnitTest"  # single test
```

Single module `:app`. Dependency versions are centralized in `gradle/libs.versions.toml` (version catalog) — add/upgrade deps there, not inline. Java 11 source/target; **Lombok** is used heavily (`@Value`, `@RequiredArgsConstructor`, `@Slf4j`, `val`, etc.) via `compileOnly`/`annotationProcessor`. Min SDK 35, target/compile SDK 36.

There is no real test suite yet (only generated `ExampleUnitTest`/`ExampleInstrumentedTest`).

## Architecture

The whole VPN lives under `app/src/main/java/.../client/`. Flow: Android UI → `VpnService` → `BaseClient` (Netty bootstrap) → encrypted custom protocol over the network on one side, and the OS TUN device on the other.

### Entry points
- `MainActivity.kt` — single screen (XML `main_layout`, a connect button + TCP/UDP switch). Calls `VpnService.prepare()` then `startService` with a `USE_TCP` extra. Compose deps exist but the live UI is classic Views.
- `client/VpnMobileClientService.java` — the `VpnService`. Holds **hardcoded config** (server address, client ID, AES key, virtual IPs, subnet). Builds the TUN via `Builder` (`addRoute 0.0.0.0/0` = capture all traffic), wraps it in `AndroidVpnTunDevice`, and runs a `TcpClient` or `UdpClient` on a background thread.

### Client core (`client/base`)
- `Client` — `start()`/`stop()` interface.
- `BaseClient` — shared lifecycle for both transports. Creates the Netty `MultiThreadIoEventLoopGroup`, opens the transport socket (`openChannel`, **protected via `VpnService.protect()`** so VPN traffic itself isn't routed back into the tunnel), bootstraps the channel, schedules the `Connector`, starts the TUN reader thread, and joins. Subclasses implement `bootstrap`, `getType`, `openChannel`, and optionally `scheduleWatchdog`.
- `ClientContext` — immutable bag passed into the Netty pipeline (server addr, tun, encryptor supplier, connector, clientId).
- `VpnPacketHandler` — the inbound protocol state machine, shared by TCP and UDP. Handles `CONNECT_ACK` (sets virtual IP on the TUN, marks CONNECTED), `KEEP_ALIVE` (replies with `KEEP_ALIVE_ACK`, refreshes liveness), `DISCONNECT`, and `DATA_TRANSFER` (writes decrypted IP packets to the TUN).
- `ClientTunReader` — reads IP packets off the TUN and injects them into the pipeline as `DATA_TRANSFER` user events.

### Transports (`client/tcp`, `client/udp`)
Both subclass `BaseClient` and define a Netty pipeline in their `*Initializer`. Note the **outbound path uses Netty user events**, not normal writes: `Connector`/`ClientTunReader` call `fireUserEventTriggered(packet)`, and an event handler (`TcpClientEventHandler` / `UdpOutChannelHandler`) gates on connection state and does the actual `writeAndFlush`. This is how reconnect logic and "drop packets while disconnected" are enforced.
- **TCP**: pipeline is encoder → event handler → `DelimiterBasedFrameDecoder` (frames split on the `RA-V` signature) → decoder → `TcpVpnPacketHandler`. Connection loss is detected by the socket itself.
- **UDP**: connectionless; uses `UdpInChannelHandler`/`UdpOutChannelHandler` with `DatagramPacket`s, and additionally schedules a `ConnectionWatchdog`.

### Connection lifecycle (`client/event`)
- `Connector` (Runnable, scheduled every `CONNECTION_INTERVAL_SEC`=3s) — fires a `CONNECT` packet whenever state is `DISCONNECTED`. Holds the `volatile ConnectionState` and `lastAlive` timestamp.
- `ConnectionWatchdog` (UDP only) — if no keep-alive arrives within `KEEP_ALIVE_TIMEOUT` (15s) while CONNECTED, flips state back to `DISCONNECTED`, which makes `Connector` reconnect. TCP relies on socket teardown instead.

### Protocol & codecs (`client/common/proto`, `client/common/converters`)
Custom binary framing: `[ "RA-V" signature ][ 1-byte type ][ payload ]`. Packet types in `VpnPacketType` (CONNECT/CONNECT_ACK/DISCONNECT/KEEP_ALIVE/KEEP_ALIVE_ACK/DATA_TRANSFER). Each type has a `*Packet` class with `fromBytes`/payload. `VpnPacketEncoder`/`VpnPacketDecoder` are the Netty adapters that delegate to a `PacketEncryptor`. Wire size limits live in `client/common/Constants.java` (`MAX_PACKET_SIZE` 1472; `MAX_VPN_PACKET_SIZE` is what's left for the inner IP packet after signature/type/IV/padding and is used as the TUN MTU).

### Crypto (`client/common/crypto`)
`PacketEncryptor` strategy chosen via `EncryptorFactory.ofType(EncryptorType, base64Key)`: `AES` (AES-128-GCM with IV+padding, requires the key), `XOR`, or `DUMMY` (passthrough). `BaseEncryptor` owns signature validation and payload parsing; note `decryptHeadless`/`parseRawHeadless` (used after TCP framing strips the delimiter) vs full `decrypt` (UDP, where each datagram still carries the signature).

### TUN device (`client/common/tun`)
`VpnTunDevice` interface abstracts the tunnel; `AndroidVpnTunDevice` wraps the `ParcelFileDescriptor` from `VpnService.Builder` and **rebuilds the TUN when the server assigns a virtual IP** in `setVirtualIp`. `TunReaderHandler` runs the blocking read loop on its own thread; `IpHeaderParser` extracts src/dst from outgoing IP packets to build `DATA_TRANSFER` packets.

## Conventions & gotchas
- New networking/protocol code is **Java + Lombok**; only UI/theme is Kotlin. Match the surrounding style (e.g. `lombok.val`, constructor injection via `@RequiredArgsConstructor`).
- Logging is SLF4J → `logback-android` (config in `app/src/main/assets/logback.xml`), plus some `android.util.Log` in the service/activity.
- Runtime config (server, client ID, AES key, virtual IPs) is currently **hardcoded constants in `VpnMobileClientService`**, not from `DataStore`/settings yet, despite `datastore-preferences` being a dependency.
- `MainActivity.onActivityResult` is the deprecated API (there's a TODO to move to `registerForActivityResult`).
</content>
</invoke>
