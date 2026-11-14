# Origo

Real player IPs behind [Zeraph](https://zeraph.app), without PROXY protocol and without a list of edge IPs to maintain.

Supports Minecraft **1.21.11 – 26.3** (Java 21+).

## Setup

Drop `Origo.jar` into `plugins/` (or `mods/` on Fabric/NeoForge) and restart. That's it.

Keep **Proxy protocol** off for the backend in the Zeraph panel, and your server's own PROXY protocol setting off too.

## How it works

For backends without proxy protocol, the Zeraph edge appends the player's address to the hostname in the Minecraft handshake. Origo reads it, sets it as the connection's address and removes it, so your server and plugins see the real IP and the hostname the player typed.

Origo downloads Zeraph's edge addresses from zeraph.app and only trusts the appended address on connections from an edge.

## Config

`config.yml` in the plugin's folder (`config/origo/` on Fabric/NeoForge):

```yaml
# Refuse connections that don't come through a Zeraph edge.
edge-only: false
```

## Build

```sh
./gradlew clean build   # build/libs/Origo-<version>.jar
```

## License

[MIT](LICENSE)
