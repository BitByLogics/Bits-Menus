<div align="center">

## Bit's Menus
[![Build](https://img.shields.io/github/actions/workflow/status/BitByLogics/Bits-Menus/maven.yml?branch=master)](https://github.com/BitByLogics/Bits-Menus/actions)
![Issues](https://img.shields.io/github/issues-raw/BitByLogics/Bits-Menus)
[![Stars](https://img.shields.io/github/stars/BitByLogics/Bits-Menus)](https://github.com/BitByLogics/Bits-Menus/stargazers)

A developer-friendly library for building interactive, paginated, config-driven inventory menus on Paper and Folia.
</div>

## Features
* A `Menu` is a shared definition. Every open creates a `MenuSession` with its own inventories, viewers, placeholders and update task.
* Menus built in code with `MenuBuilder` or loaded from YAML.
* Items with click actions, click and view requirements, cooldowns, update providers and per-menu placeholders.
* Automatic pagination with next and previous page items.
* Menu flags to allow input, removal, lower-inventory interaction and more.
* Built-in click actions: run console or player commands, send messages, play sounds and close the inventory.
* A ready-made confirmation menu.
* Paper and Folia support through FoliaLib.

## Requirements
* Java 21
* Paper 1.21.11 or Folia
* [Bit's Utils](https://github.com/BitByLogics/Bits-Utils)

## Installation

```xml
<repository>
    <id>minebit</id>
    <url>https://repo.minebit.dev/releases</url>
</repository>

<dependency>
    <groupId>net.bitbylogic</groupId>
    <artifactId>bits-menus</artifactId>
    <version>2.0.0</version>
</dependency>
```

The library is not shaded. Its dependencies (Bit's Utils, FoliaLib) come in transitively, so your build controls their versions. Shade this library and those dependencies into your plugin, and relocate `net.bitbylogic.utils` and `com.tcoded.folialib` to your own package.

## Quick start

Register the listener once in your plugin:

```java
public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
    }

}
```

Build and open a menu:

```java
MenuItem closeItem = new MenuItem("close")
        .item(new ItemStack(Material.BARRIER))
        .withSlot(4)
        .withInternalAction(InternalClickAction.CLOSE_INVENTORY, "");

Menu menu = new MenuBuilder("example", Component.text("Example"), MenuRows.THREE)
        .withItem(closeItem)
        .build();

menu.open(player, plugin);
```

`Menu#open` creates a new `MenuSession` for that player. To share one live menu between players, create the session yourself:

```java
MenuSession session = menu.createSession();
session.withPlaceholder(Placeholder.unparsed("player", player.getName()));
session.open(player, plugin);
session.open(otherPlayer, plugin);
```

Item update providers receive the session, so an item can render differently per session:

```java
new MenuItem("balance").withSlot(13).updatable(true)
        .updateProvider((session, item) -> createBalanceItem(session));
```

Or load a menu from `plugins/<YourPlugin>/menus/<id>.yml`:

```java
Optional<Menu> menu = Menu.getFromFile(plugin.getDataFolder(), "example");
```

### Pagination

Give the menu `Valid-Slots`, add content with `MenuSession#addItem` or `addItemStack`, and add navigation items with `MenuItem#nextPageButton(slots)` and `previousPageButton(slots)`. Pages are appended to the session as they fill, up to `maxInventories`. In YAML, items with the ids `Next-Page-Item` and `Previous-Page-Item` become the navigation buttons, using their own `Slot(s)` or the `Next-Page-Slots` and `Previous-Page-Slots` metadata.

### Flags

`ALLOW_INPUT`, `ALLOW_REMOVAL`, `LOWER_INTERACTION`, `DISABLE_UPDATES`, `ALWAYS_DISPLAY_NAV` and `DEBUG`. Unknown flags in a YAML menu are skipped with a warning.

## Folia
Scheduling goes through [FoliaLib](https://github.com/TechnicallyCoded/FoliaLib), so the same code runs on Paper and Folia.

* Opening a menu and the close handling run on the player's own region.
* The `RUN_CONSOLE_COMMAND` action runs on the next tick of the global region, so it is delayed by one tick.
* The periodic item update task runs on the global region while a menu has viewers.

`folia-supported: true` in your `plugin.yml` is only needed if you actually run on Folia. Paper servers ignore it.

## Notes
* `Menu`, `MenuItem` and `MenuData` are definitions. Their lists are copy-on-write, so sessions on other threads can read them while you configure them. Content added at runtime belongs on the session, not the definition.
* `MenuSession#getItems()`, `getViewers()` and `getInventories()` return read-only snapshots.
* Items without slots are not rendered. Use `MenuSession#addItem` to place them.
* `Menu.getFromFile` reads the file synchronously.
* An invalid menu size is clamped to a multiple of 9 between 9 and 54, with a warning.

## Contributing and security
See [CONTRIBUTING.md](CONTRIBUTING.md). Report vulnerabilities privately to security@minebit.dev, as described in [SECURITY.md](SECURITY.md).

## License
MIT. See [LICENSE](LICENSE).

## Building

```
mvn clean verify
```

`verify` runs the tests, a JaCoCo coverage report (`target/site/jacoco`) and SpotBugs, and builds the main, sources and Javadoc jars.

## Releasing

Set the version in `pom.xml`, then push a tag named exactly the version, with no prefix (for example `2.0.0`). The release workflow checks the tag matches the pom, deploys to the Minebit repository and creates a GitHub release. It needs the repository secrets `MINEBIT_USERNAME` and `MINEBIT_PASSWORD`.
