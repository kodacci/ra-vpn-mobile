# RA VPN Mobile

An Android VPN client (`pro.ra_tech.ra_vpn.mobile`) implementing a custom encrypted VPN protocol over UDP or TCP. It tunnels all device traffic through an Android `VpnService` and talks to a companion RA VPN server using a lightweight binary protocol built on [Netty](https://netty.io/).

## Features

- **Full-device tunneling** — routes `0.0.0.0/0` through an Android TUN interface created with `VpnService.Builder`.
- **Two transports** — UDP (default) or TCP, selectable from the UI. UDP uses a keep-alive watchdog for liveness; TCP relies on socket teardown.
- **Custom binary protocol** — framed packets (`RA-V` signature + type byte + payload) with CONNECT / KEEP_ALIVE / DISCONNECT / DATA_TRANSFER semantics and automatic reconnection.
- **Encryption** — pluggable packet encryptors: AES-128-GCM (default), XOR, or passthrough for debugging.
- **Loop protection** — the transport socket is excluded from the tunnel via `VpnService.protect()` so VPN traffic isn't routed back into itself.

## Requirements

- Android SDK (min SDK **35**, target/compile SDK **36**)
- JDK 11+ (Java 11 source/target)
- A running RA VPN server to connect to

## Building

Create `local.properties` in the project root pointing at your Android SDK:

```properties
sdk.dir=/path/to/android-sdk
```

Then use the Gradle wrapper:

```bash
./gradlew assembleDebug        # build debug APK
./gradlew installDebug         # build + install on a connected device/emulator
./gradlew test                 # JVM unit tests
./gradlew connectedAndroidTest # instrumented tests (needs device/emulator)
./gradlew lint                 # Android lint
```

## Configuration

Runtime configuration (server address, client ID, AES key, virtual IPs, subnet) is currently **hardcoded** in `client/VpnMobileClientService.java`. Edit those constants to point the client at your server before building. Moving this to persisted settings (`DataStore`) is planned.

## Architecture

The UI layer is a thin Kotlin screen (`MainActivity.kt` — a connect button and a TCP/UDP switch) that starts/stops the VPN service. Everything else is Java under `app/src/main/java/pro/ra_tech/ra_vpn/mobile/client/`:

```
Android UI → VpnService → BaseClient (Netty) → encrypted custom protocol ⇄ server
                                   ↕
                              TUN device (OS traffic)
```

| Package | Purpose |
|---|---|
| `client/base` | Shared client lifecycle (`BaseClient`), Netty pipeline context, the inbound protocol state machine (`VpnPacketHandler`), and the TUN reader |
| `client/tcp`, `client/udp` | Transport-specific Netty pipelines. Outbound writes go through Netty user events, gated on connection state |
| `client/event` | `Connector` (periodic reconnect while disconnected) and `ConnectionWatchdog` (UDP keep-alive timeout) |
| `client/common/proto` | Packet types, binary framing, Netty encoder/decoder |
| `client/common/crypto` | `PacketEncryptor` strategies (AES / XOR / dummy) via `EncryptorFactory` |
| `client/common/tun` | TUN device abstraction; rebuilds the TUN when the server assigns a virtual IP |

### Wire protocol

Every packet is `[ "RA-V" signature ][ 1-byte type ][ payload ]`, encrypted by the configured `PacketEncryptor`. Max datagram size is 1472 bytes; the TUN MTU is derived from what remains after signature, type, IV, and padding overhead.

### Connection lifecycle

A `Connector` runs every 3 seconds and sends `CONNECT` whenever the state is `DISCONNECTED`. The server replies with `CONNECT_ACK` carrying the assigned virtual IP. While connected, the server sends `KEEP_ALIVE` packets which the client acknowledges; on UDP, missing keep-alives for 15 seconds triggers a reconnect.

## Tech stack

- **Kotlin** for the UI, **Java 11 + Lombok** for all networking/protocol code
- **Netty** for async I/O on both transports
- **SLF4J + logback-android** for logging (config in `app/src/main/assets/logback.xml`)
- Gradle version catalog (`gradle/libs.versions.toml`) for dependency management
