# Rapid Holster v1.0.0

Initial release package:

- Animated back holstering with automatic staff, bow and two-handed presets.
- Movable sheathe/unsheathe button showing the equipped weapon.
- Automatic weapon drawing during action animations.
- Searchable custom weapons with individually saved placement controls.
- Saved weapon sections collapsed on launch; new entries open for editing.
- Persistent Hide cape checkbox at the top of the sidebar.
- Dark sidebar styling and matching leather holster icons.

## Validation status

Java 11 syntax parsing and package integrity checks passed. Earlier iterations
were tested in-game by the author. Full dependency compilation and live-client
testing of this release package have not been performed in this environment.
Run `gradlew.bat clean build` and `gradlew.bat runClient` before submission.

## Repository contents

Upload the contents of the rapid-holster-v1.0.0 folder to the repository root,
including the Gradle wrapper, src, icon.png, runelite-plugin.properties, licence
and third-party notices. Do not upload the ZIP itself, build outputs or .gradle.
This package does not publish a GitHub release or submit to Plugin Hub.
