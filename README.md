# Rapid Holster

Version 1.0.0 · by RapidUrsa

Cosmetic back holstering with a movable equipped-item icon button and animated
torso attachment. Run `gradlew.bat clean runClient` from this project on Windows.

## Detection and placement

Staffs, battlestaffs, tridents, sceptres and wands are recognised by item name,
with explicit support for Tumeken's shadow and Eye of ayak. Bows use the bow
preset; crossbows do not use that preset. Other items marked two-handed in
RuneLite equipment stats use the two-handed preset. Missing equipment metadata
can leave an unusual weapon unsupported. Models are loaded from the game cache.

The three shared settings sections use the tested defaults:

| Preset | Sideways | Height | Forward | Pitch | Yaw | Roll | Scale |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Two-handed | -5 | 142 | 15 | -95 | 90 | -41 | 100 |
| Staff | 2 | 148 | 15 | -77 | 90 | -41 | 100 |
| Bow | -1 | 149 | 24 | -98 | 90 | -41 | 100 |

## Drawing and controls

Left-click the item icon to toggle holstering. Alt-drag repositions the overlay.
Supported weapons draw during any action animation, including attacks, spells,
eating and emotes. After the action, they stay drawn for a short interval based
on weapon attack speed before returning to the back. Walking/running uses the
existing torso attachment.

## Custom weapons sidebar

The sidebar uses RuneLite dark colours, compact search controls, and orange
weapon section headings.

Open the leather pistol-holster icon in the sidebar. While logged in, type a
weapon name or item ID and press Enter or Search. Select the matching item and
click Apply / Add weapon (or press Enter on the result).

The weapon appears in its own collapsible section. Equip and holster it, then
adjust Sideways, Height, Forward, Pitch, Yaw, Roll and Scale live. Each weapon
saves independently. Saved weapon sections start collapsed on launch; newly added
weapons open immediately for positioning. Reset copies the category defaults; Remove deletes only
that weapon's override. Existing saved custom placements are loaded automatically.

The plugin logo is icon.png; the matching sidebar icon is packaged as a resource.
The original vector artwork is icon.svg.

Use **Hide cape** at the top of the sidebar to hide your equipped cape visually.
The choice is remembered and also applies while your weapon is drawn. Untick it
or disable Rapid Holster to restore the cape; equipment and bonuses are unchanged.

## Build and run locally

Use JDK 17 or newer to run the included Gradle wrapper. The plugin targets Java 11.

On Windows, open a terminal in the extracted project folder:

```bat
gradlew.bat clean build
gradlew.bat runClient
```

On Linux or macOS:

```sh
./gradlew clean build
./gradlew runClient
```

## Compatibility

The visual changes are local to your client. Equipment and combat bonuses are
unchanged. Other appearance-changing plugins may affect the same player model.
Unusual weapon shapes may need a custom placement in the sidebar.

## Licence

BSD 2-Clause. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
