**AutoModpack Velocity** is a Velocity plugin companion to [AutoModpack](https://github.com/Skidamek/AutoModpack/) server instances,
enabling you to use it in your Velocity proxy network.

<p align="center">
    <a href="https://github.com/Skidamek/AutoModpackVelocity/releases"><img src="https://img.shields.io/github/downloads/skidamek/automodpackvelocity/total?style=round&logo=github" alt="GitHub Total Downloads"></a>
    <a href="https://modrinth.com/mod/automodpackvelocity"><img src="https://img.shields.io/modrinth/dt/lCCrKinv?logo=modrinth&label=&style=flat&color=242629" alt="Modrinth Downloads"></a>
</p>

> Before reading this README, read [AutoModpack's README](https://github.com/Skidamek/AutoModpack/) to understand the basics of AutoModpack.

## 🛠️ How does it work

This plugin behaves as a proxy for AutoModpack. It allows you to use AutoModpack in your Velocity proxy network, but it does not provide any additional features.

## ⚠️ Security and Trust!

> With great power comes great responsibility.

We highly recommend following [Velocity's own security guide](https://docs.papermc.io/velocity/security/) as a basis.
As this plugin is a proxy on its own you need to securely block direct connections to your backend servers, including
the modpack server connection port. Also, keep in mind [AutoModpack's security section](https://github.com/Skidamek/AutoModpack/#%EF%B8%8F-security-and-trust) before using the mod.

## 🚀 Getting Started is a Breeze!
Installing AutoModpack Velocity is as simple as installing any other Velocity plugin.

1.  Setup your Velocity ([guide](https://docs.papermc.io/velocity/getting-started/)) and AutoModpack servers ([guide](https://github.com/Skidamek/AutoModpack/#-getting-started-is-a-breeze)) instances
2.  Download the AutoModpack Velocity from the releases page on [GitHub](https://github.com/Skidamek/AutoModpackVelocity/releases), or [Modrinth](https://modrinth.com/plugin/automodpackvelocity).
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
| `modpackHost`      | `true`      | Enables or disables the internal host server that serves the modpack.                                |

### Network & Hosting

| Setting          | Default | Description                                                                                 |
|------------------|---------|---------------------------------------------------------------------------------------------|
| `bindAddress`    | `""`    | The local IP to bind the host to. Empty binds to 0.0.0.0 or ::0. Ignored if bindPort is -1. |
| `bindPort`       | `-1`    | The port to listen on. -1 uses the Minecraft Server's port (recommended).                   |
| `bandwidthLimit` | `0`     | Upload speed limit in Mbps (0 = unlimited).                                                 |

## 🙏 Huge Thanks to Our Supporters!

AutoModpack wouldn't be where it is without the amazing community!

*   **All the [AutoModpack](https://github.com/Skidamek/AutoModpack/graphs/contributors) and [AutoModpack Velocity](https://github.com/Skidamek/AutoModpackVelocity/graphs/contributors) contributors** who have helped improve the mod!
*   **[duckymirror](https://github.com/duckymirror), Juan, cloud, [Merith](https://github.com/Merith-TK), [SettingDust](https://github.com/SettingDust), Suerion, and griffin4cats** for their invaluable help with testing, code, and ideas!
*   **HyperDraw** for creating the mod icon!
*   **All the generous supporters on [Ko-fi](https://ko-fi.com/skidam)** - your support means the world!

## 💖 Contribute and Make AutoModpack Even Better!

We love contributions! Whether it's code, bug reports, documentation improvements, or just spreading the word, your help is welcome.

**Ready to contribute? See our [CONTRIBUTING.md](CONTRIBUTING.md) for details!**
