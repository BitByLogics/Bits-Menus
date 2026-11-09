# Changelog

## 2.0.0

### Breaking
- `Menu` is now only the definition (id, title, size, `MenuData`, items). The live state moved to the new `MenuSession`, which is the `InventoryHolder`. Code that checks `holder instanceof Menu` must check `MenuSession` and call `getMenu()`.
- `Menu#open(player, plugin)` creates a new session per call, so each player gets their own inventories. Use `Menu#createSession()` to share one session between players.
- Moved from `Menu` to `MenuSession`: `getInventory`, `getInventories`, `getViewers`, `open`, `addAndSetItem` (now `addItem`, returns a boolean), `addItemStack`, `getItem(inventory, slot)`, `getItems(inventory, slot)`, `getMenuInventory` (now `getPage`), `getInventoryIndex` (now `getPageIndex`), the capacity methods and `getVanillaItems`. `getGlobalMenu`, `getNextAvailableSlot`, `generateNewInventory`, `clone` and the lock getters are removed.
- `MenuItem` is a pure definition. `locked`, `global`, `saveSlots`, `sourceInventories` and `setMenu` are removed, so setters no longer silently do nothing.
- `MenuItemUpdateProvider.requestItem` and `MenuViewRequirement.canView` take the `MenuSession` (and the page) instead of a bare `Inventory` or `Menu`.
- Pagination buttons are `MenuItem#nextPageButton(slots)` and `previousPageButton(slots)`. The `Next-Page-Item` and `Previous-Page-Item` ids and the `Next-Page-Slots` and `Previous-Page-Slots` metadata still work when loading YAML.
- `Menu`, `MenuItem` and `MenuData` lists are copy-on-write. `MenuItem#getInternalActions` returns a `Map`.
- `TitleUpdateTask` and `InventoryHandler` are removed. `MenuFlag.DISABLE_TITLE_UPDATE` is deprecated and has no effect.
- `MenuInventory` holds the slot placements of a page. `MenuInventory.getMenu()` is removed.
- `MenuBuilder` no longer throws on an invalid size. It clamps it to a multiple of 9 between 9 and 54 and logs a warning.
- Saved menus store the title as a MiniMessage string.
- The library is no longer shaded. Dependencies are transitive.
- The `RUN_CONSOLE_COMMAND` action runs on the next tick of the global region, so it is delayed by one tick.
- The project depends on Bit's Utils 2.4.0 from the Minebit repository instead of JitPack.

### Added
- Folia support through FoliaLib (`MenuScheduler`). Menu opening and close handling run on the player's region, and the update task on the global region.
- `MenuSession` with per-session placeholders, `appendPage`, `refresh`, `refreshItem`, `addViewer` and `removeViewer`, plus `NextPageAction` and `PreviousPageAction`.
- MIT license, unit tests, JaCoCo coverage, SpotBugs analysis, source and Javadoc jars, Dependabot, a GitHub Actions build with a dependency review on pull requests, and a tag-driven release workflow.
- A real README, contributing guide and security policy.

### Fixed
- Viewers, items and inventories were changed from several threads without synchronisation. The update task could hit a `ConcurrentModificationException`.
- Opening a menu mutated the shared `MenuItem` definitions (slots, source inventories, locked state), and the page navigation requirements were added again on every generated page.
- An unknown action name in a menu file stored a `null` key. It is now skipped with a warning.
- The update task could be started twice when two players opened a menu at the same time, and could be left without a null guard on close.
- Saving a menu wrote the title as a `Component` object, so the file could not be loaded again.
- An unknown flag in a menu file threw an `IllegalArgumentException`. It is now skipped with a warning.
- `Menu.open` threw on a page below 1 and ignored a page above the last silently. Both now log a warning and do nothing.
- Updating an item that only has an update provider threw a `NullPointerException`.
- `saveToFile` printed a stack trace and ignored a failed `mkdirs()`. It now logs through the logger and reports the failure.
- The inventory refresh after close no longer runs for a player who went offline.
