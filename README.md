# Mob Heart Display

A client-side Fabric mod that shows a heart bar above mobs, just like your own health bar.

<!-- Screenshot: replace with an image from the repo, e.g. ![Hearts above mobs](docs/screenshot.png) -->

## Features

- **Vanilla-style hearts** above every living mob, with the same full and half hearts as your HUD
- **Big mobs stay readable**: health is split into bars of up to 5 rows, with a number showing how many bars are left
- **Effect hearts**: poisoned (green), withered (black) and frozen (light blue), like the vanilla HUD
- **Absorption**: golden hearts for players, in an extra row like in the HUD
- **Damage animation**: lost hearts shrink away instead of vanishing, with adjustable speed
- **Designs**: different heart styles per category (players, bosses, tamed animals, undead, hostile and passive mobs) or per mob, fully customizable with resource packs
- **Built-in "Mob Hearts" resource pack** with a colored design for each category
- **Filters**: hostile/passive mobs, only damaged mobs, hide after some time without damage, maximum range, blacklist and whitelist
- **Settings screen** (also through [Mod Menu](https://modrinth.com/mod/modmenu)) and `/mhd` commands
- **Client-side only**: works on any server, nothing to install there

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Mod loader | [Fabric Loader](https://fabricmc.net/use/) 0.20.1 or newer |
| Required | [Fabric API](https://modrinth.com/mod/fabric-api) |
| Optional | [Mod Menu](https://modrinth.com/mod/modmenu) (settings button in the mod list) |

## Installation

1. Install Fabric Loader for Minecraft 26.3.
2. Put `mob-heart-display-<version>.jar` and Fabric API into your `mods` folder.
3. Start the game.

## Usage

| Key / command | What it does |
|---|---|
| `H` | Turn the hearts on or off |
| *Open Settings* (unbound by default) | Opens the settings screen; set a key under *Options → Controls → Key Binds* |
| `/mhd config` | Opens the settings screen |
| `/mhd` | Lists all settings and their current values |
| `/mhd <setting> [value]` | Shows or changes a setting, e.g. `/mhd heartRows 2` |
| `/mhd blackList add\|remove <mob>` | Adds or removes a mob from the blacklist (same for `whiteList`) |
| `/mhd blackList list` | Shows the blacklist (same for `whiteList`) |
| `/mhd design` | Lists the available designs and your own choices |
| `/mhd design category <category> <design>` | Uses a design for a whole category |
| `/mhd design entity <mob> <design>` | Uses a design for one mob |
| `/mhd design reset category\|entity\|all` | Removes your own design choices |

### Settings

| Setting | Default | Description |
|---|---|---|
| Show Hearts | On | Show hearts at all |
| Hostile Mobs / Passive Mobs | On | Show hearts for these groups |
| Show Yourself | Off | Show your own hearts (third-person view) |
| Only Damaged | Off | Only show mobs that are not at full health |
| Bar Count | On | Number below the hearts when a mob has more than one bar |
| Count Prefix | Off | Show `x5` instead of `5` |
| Divider Lines | On | Lines left and right of the number (if the design has them) |
| Effect Hearts | On | Poisoned, withered and frozen hearts |
| Absorption | On | Golden absorption hearts (players only) |
| Damage Animation | On | Lost hearts shrink away |
| Anim. Speed | 1.00 | Speed of the damage animation (0.25 to 4) |
| Blacklist / Whitelist | Off | Hide the listed mobs / show only the listed mobs |
| Height Offset | 0.00 | Move the hearts up or down |
| Heart Rows | 1 | Rows per bar (1 to 5) |
| Hide After (s) | Off | Hide the hearts some seconds after the mob was last hurt |
| Max Range | Unlimited | Only show hearts within this distance |

Some rules are fixed: nothing is shown while the HUD is hidden (F1), invisible mobs get no hearts, and other players get no hearts while sneaking or in Creative or Spectator mode.

Settings are saved in `config/mob_health_display.json`, your own design choices in `config/mob_health_display_designs.json`.

## Custom designs (resource packs)

Designs are folders of textures inside a resource pack:

```
assets/mob_health_display/
├── designs.json
└── textures/designs/<design name>/
    ├── container.png        (required) empty heart / background
    ├── full.png             (required) full heart
    ├── half.png             (required) half heart
    ├── divider.png          (optional) line next to the bar count, 16x3, stretched
    ├── lost.png             (optional) damage animation, see below
    ├── poisoned_full.png    (optional) also: poisoned_half, withered_full, withered_half,
    ├── ...                             frozen_full, frozen_half, absorbing_full, absorbing_half
    └── poisoned_lost.png    (optional) damage animation per heart type
```

- Heart textures are **9x9 pixels**.
- Missing effect textures fall back to the vanilla hearts of the game.
- `lost.png` is a vertical strip of 9x9 frames (any number of frames), played from top to bottom. Without it, the full heart simply shrinks.

`designs.json` decides which mob gets which design:

```json
{
  "default": "vanilla",
  "categories": {
    "player": "player",
    "boss": "boss",
    "tamed": "tamed",
    "undead": "undead",
    "hostile": "hostile",
    "passive": "passive"
  },
  "entities": {
    "minecraft:warden": "warden"
  }
}
```

`"vanilla"` always means the normal game hearts. Order of priority: your own choice for a mob → pack entry for the mob → your own choice for the category → pack entry for the category → pack default → vanilla. A higher resource pack replaces the whole `designs.json` of lower ones. Press `F3 + T` to reload after changes.

## Known limitations

- The game only tells the client about **effects of other mobs** through their particles. Effects given without particles (e.g. `/effect ... true`) can't be detected.
- **Absorption** is only known for players, so mobs never show golden hearts.
- **Shaders** (Iris) have not been tested yet. Feedback is welcome!

## Bugs and ideas

Found a bug or have an idea? Please open an [issue](https://github.com/Kirtilot/mob-health-display/issues/new/choose).

## Credits

Inspired by *Mob Health Indicators*, which is no longer updated for new Minecraft versions.

## License

[MIT](LICENSE.txt)
