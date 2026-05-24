**AutoModpack Velocity** is a Velocity companion plugin for [AutoModpack](https://modrinth.com/project/automodpack/), enabling you to use it on your Velocity proxy network.

<p align="center">
    <a href="https://github.com/Skidamek/AutoModpackVelocity/releases"><img src="https://img.shields.io/github/downloads/skidamek/automodpackvelocity/total?style=round&logo=github" alt="GitHub Total Downloads"></a>
    <a href="https://modrinth.com/mod/automodpackvelocity"><img src="https://img.shields.io/modrinth/dt/lCCrKinv?logo=modrinth&label=&style=flat&color=242629" alt="Modrinth Downloads"></a>
</p>

> Before installing this plugin, you should have at least basic knowledge of [AutoModpack](https://modrinth.com/project/automodpack/) itself.

## 🛠️ How does it work

AutoModpack normally expects the Minecraft server and the AutoModpack file host to be directly reachable by the client. In a Velocity network, the client connects to the proxy first, while the actual modded servers live behind it. This plugin bridges that gap by making Velocity look like the AutoModpack host and forwarding the real file traffic to the correct backend server.

There are two parts involved:

1. **Login-phase communication:** AutoModpack sends its connection details during Minecraft's login plugin message phase. Velocity does not normally forward those backend login plugin messages to the player after the proxy login has completed, so this plugin depends on [LoginPhaseProxy](https://modrinth.com/plugin/loginphaseproxy) to proxy that login-phase communication between the player and the selected backend server.
2. **AutoModpack traffic proxying:** When a backend server sends AutoModpack's `automodpack:data` login message, AutoModpack Velocity reads the backend's AutoModpack host port, stores it for that backend server, and rewrites the message sent to the client. Instead of telling the client to connect directly to the backend, it tells the client to connect to the Velocity proxy's AutoModpack endpoint.

After that, the client downloads the modpack exactly as it would with standalone AutoModpack, but its TCP connection goes through this plugin:

1. The client connects to the proxy-provided AutoModpack host.
2. The client sends AutoModpack's magic hostname handshake, which lets the plugin know which Velocity virtual host the player used.
3. The plugin resolves that hostname using Velocity's forced-hosts and fallback server order, then picks the matching backend server.
4. Using the cached AutoModpack port from that backend's login message, the plugin opens a TCP connection to the backend server's AutoModpack host.
5. From that point on, bytes are forwarded both ways, so metadata and modpack files still come from the backend AutoModpack server while the client only needs to reach Velocity.

By default, `port` is `-1`, which means AutoModpack traffic shares the same public port as Velocity. In that mode, this plugin injects an early frontend handler into Velocity's network pipeline: normal Minecraft connections continue through Velocity, while AutoModpack connections are detected by their magic handshake and diverted into the TCP proxy. If you configure a dedicated `port`, the plugin starts its own Netty listener for AutoModpack traffic instead.

This plugin does not generate modpacks, decide which files belong in them, or add client-side AutoModpack features. Backend servers still run the normal AutoModpack mod and remain responsible for producing and hosting the modpack data; this plugin only makes that workflow usable through a Velocity proxy.

## ❗️Known Limitations

### Modpack updates are not triggered on server switches

AutoModpack checks for modpack updates when the Minecraft client launches and when it connects to a server. When switching between backend servers within a Velocity network, no new connection is established from the client's perspective, so no update check is triggered.

**Workaround 1 (preferable):** Install AutoModpack on the lobby server with `modpackHost` disabled, and point its `addressToSend` and `portToSend` to the backend modded server's AutoModpack host. The lobby itself does not need to share the same mod loader or mods. Note that `addressToSend` here refers to the backend server's local IP, not the Velocity host.

**Workaround 2:** Use a dedicated lobby server as the entry point with AutoModpack installed, and remove AutoModpack from all other backend servers. Players will receive modpack updates on initial connection to the lobby, before being routed to any other server.

## ⚠️ Security and Trust!

> With great power comes great responsibility.

We highly recommend following [Velocity's own security guide](https://docs.papermc.io/velocity/security/) as a basis.

Since this plugin acts as a proxy itself, you should securely block direct connections to your backend servers, including the modpack server connection port. You should also be aware of [AutoModpack's security trade-offs](https://modrinth.com/project/automodpack/).

## 🚀 Getting Started
Installing AutoModpack Velocity is as simple as installing any other Velocity plugin.

1.  Setup your Velocity ([guide](https://docs.papermc.io/velocity/getting-started/)) and backend servers with AutoModpack installed ([guide](https://moddedmc.wiki/en/project/automodpack/latest/docs/quick-start)).
2.  Download the AutoModpack Velocity plugin from the releases page on [GitHub](https://github.com/Skidamek/AutoModpackVelocity/releases), or [Modrinth](https://modrinth.com/project/automodpackvelocity).
3.  Download the LoginPhaseProxy dependency from the releases page on [GitHub](https://github.com/caiostoduto/LoginPhaseProxy/releases), or [Modrinth](https://modrinth.com/plugin/loginphaseproxy).
4.  Place both downloaded files into the `/plugins/` folder of your proxy.
5.  Restart your proxy.

## Proxy Configuration

**File Location:** `/plugins/automodpack/automodpack-proxy.json`

These settings refers only to the **AutoModpack Velocity**, not the individual backend servers. For AutoModpack configuration on your backend servers, see [AutoModpack's documentation](https://moddedmc.wiki/en/project/automodpack/latest/docs).

### General Settings

| Setting            | Default     | Description                                                                                                                    |
|--------------------|-------------|--------------------------------------------------------------------------------------------------------------------------------|
| `DO_NOT_CHANGE_IT` | `<version>` | Internal Use Only. Used to auto-migrate config versions during updates. Do not modify this manually.                           |
| `proxyHost`        | `true`      | Enables or disables the plugin internal HTTP server that proxies modpack traffic to clients on behalf of your backend servers. |

### Network & Hosting

| Setting          | Default | Description                                                                                                    |
|------------------|---------|----------------------------------------------------------------------------------------------------------------|
| `address`        | `""`    | Used with `port` to bind the proxy host when `proxyHost` is enabled, and always used to rewrite the backend `automodpack:data` packet sent to clients. Empty binds to 0.0.0.0 or ::0. Ignored for binding if `port` is -1. |
| `port`           | `-1`    | Used with `address` to bind the proxy host when `proxyHost` is enabled, and always used to rewrite the backend `automodpack:data` packet sent to clients. -1 uses the Velocity server's port for shared-port mode. |
| `bandwidthLimit` | `0`     | Upload speed limit in Mbps (0 = unlimited).                                                                    |


## 🙏 Acknowledgements

- [Caio Stoduto](https://github.com/caiostoduto), who dedicated his time and effort to creating this plugin!
