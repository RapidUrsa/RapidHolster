# Rapid Holster v2.0.0

Expanded cosmetic holstering to one-handed weapons, wands, crossbows, and equipped off-hand items.

- One-handed weapons and wands use tuned left-hip placements; crossbows have a separate preset.
- Books, defenders, and other off-hand items use the right hip; shields, wards, and bucklers sit on the back.
- Off-hand items work even when no weapon is equipped, with their own icon on the sheathe button.
- Custom gear can be searched, added, and positioned independently in the sidebar.
- Equipped models return to their normal positions during actions, when unsheathed, and when the plugin is disabled.
- Defaults include the final in-game placement values supplied by RapidUrsa.

## Release checks

On Windows, from the extracted folder, run `gradlew.bat clean build` and then
`gradlew.bat runClient`. Test weapon and off-hand holstering, attacks, walking,
running, and disabling the plugin before submitting the Plugin Hub update.
