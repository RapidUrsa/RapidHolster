# Rapid Holster

Version 2.0.1 · by RapidUrsa

Cosmetic hip and back holstering with a movable equipped-item icon button
and animated torso attachment. Run `gradlew.bat clean runClient` from this project on Windows.

## Detection and placement

Staffs, battlestaffs, tridents and sceptres are recognised by item name,
with explicit support for Tumeken's shadow and Eye of ayak. Bows use the bow
preset. Wands and one-handed melee weapons use separate presets on the
left hip. Crossbows have their own placement preset. Other items marked
two-handed in RuneLite equipment stats use the two-handed back preset. Books,
defenders and other non-shield off-hand items sit on the right hip. Shields,
wards and bucklers sit on the back. Missing
equipment metadata can leave an unusual weapon unsupported. Models are loaded
from the game cache.

The original three shared presets and the newly tuned placements are:

| Preset | Sideways | Height | Forward | Pitch | Yaw | Roll | Scale |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Two-handed | -5 | 142 | 15 | -95 | 90 | -41 | 100 |
| Staff | 2 | 148 | 15 | -77 | 90 | -41 | 100 |
| Bow | -1 | 149 | 24 | -98 | 90 | -41 | 100 |
| One-handed (left hip) | 24 | 90 | 12 | 32 | 180 | 0 | 100 |
| Wand (left hip) | 24 | 90 | 12 | 32 | 180 | 0 | 100 |
| Crossbow | -13 | 166 | 22 | 2 | 180 | -33 | 100 |
| Other off-hand (right hip) | -26 | 90 | 10 | 48 | 174 | 0 | 100 |
| Shield (back) | 0 | 158 | 22 | 0 | -88 | 0 | 85 |

## Drawing and controls

Left-click the item icon to toggle holstering. Alt-drag repositions the overlay.
Supported weapons and off-hand items draw during action animations, including attacks,
spells, eating and emotes. With a weapon equipped they stay drawn for a short
interval based on weapon attack speed before returning to the hip or back.
Walking and running use the existing torso attachment. Off-hand items can also be
holstered without a weapon; the button then displays the off-hand item icon.

## Custom gear sidebar

The sidebar uses RuneLite dark colours, compact search controls, and orange
item section headings.

Open the leather pistol-holster icon in the sidebar. While logged in, type a
weapon or off-hand item name or ID and press Enter or Search. Select the
matching item and click Apply / Add item (or press Enter on the result).

The item appears in its own collapsible section. Equip and holster it, then
adjust Sideways, Height, Forward, Pitch, Yaw, Roll and Scale live. Each item
saves independently. Saved item sections start collapsed on launch; newly added
items open immediately for positioning. Reset copies the category defaults;
Remove deletes only that item's override. Existing saved custom placements are loaded automatically.

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

Off-hand items return to the hand during actions, or when unsheathed, unequipped,
or the plugin is disabled.

## Licence

BSD 2-Clause. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
