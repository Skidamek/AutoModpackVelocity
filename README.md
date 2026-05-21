**AutoModpack Velocity** is a Velocity companion plugin for [AutoModpack](https://modrinth.com/project/automodpack/), enabling you to use it on your Velocity proxy network.

<p align="center">
    <a href="https://github.com/Skidamek/AutoModpackVelocity/releases"><img src="https://img.shields.io/github/downloads/skidamek/automodpackvelocity/total?style=round&logo=github" alt="GitHub Total Downloads"></a>
    <a href="https://modrinth.com/mod/automodpackvelocity"><img src="https://img.shields.io/modrinth/dt/lCCrKinv?logo=modrinth&label=&style=flat&color=242629" alt="Modrinth Downloads"></a>
</p>

> Before installing this plugin, you should have at least basic knowledge of [AutoModpack](https://modrinth.com/project/automodpack/) itself.

## 🛠️ How does it work

This plugin behaves as a proxy for AutoModpack traffic. It allows you to use AutoModpack on your Velocity proxy network, but it does not provide any additional features.

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

This configuration refers only to the proxy instance of AutoModpack, not the individual backend servers. For backend server configuration, see [AutoModpack's documentation](https://moddedmc.wiki/en/project/automodpack/latest/docs).

### General Settings

| Setting            | Default     | Description                                                                                          |
|--------------------|-------------|------------------------------------------------------------------------------------------------------|
| `DO_NOT_CHANGE_IT` | `<version>` | Internal Use Only. Used to auto-migrate config versions during updates. Do not modify this manually. |
| `modpackHost`      | `true`      | Enables or disables the internal proxy server that serves the modpack from your backend servers.     |

### Network & Hosting

| Setting          | Default | Description                                                                                 |
|------------------|---------|---------------------------------------------------------------------------------------------|
| `bindAddress`    | `""`    | The local IP to bind the proxy to. Empty binds to 0.0.0.0 or ::0. Ignored if bindPort is -1.|
| `bindPort`       | `-1`    | The port to listen on. -1 uses the Velocity Server's port (recommended).                    |
| `bandwidthLimit` | `0`     | Upload speed limit in Mbps (0 = unlimited).                                                 |


## 🙏 Acknowledgements

- [Caio Stoduto](https://github.com/caiostoduto), who dedicated his time and effort to creating this plugin!
