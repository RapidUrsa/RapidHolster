package com.rapidursa.holster;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(RapidHolsterPlugin.CONFIG_GROUP)
public interface RapidHolsterConfig extends Config
{
    @ConfigItem(keyName = "hideCape", name = "Hide cape", hidden = true,
        description = "Visually hide your cape; controlled from the Rapid Holster sidebar")
    default boolean hideCape() { return false; }

    @ConfigItem(keyName = "holstered", name = "Holstered",
        description = "Move the supported equipped weapon onto your back", position = 0)
    default boolean holstered() { return true; }

    @ConfigItem(keyName = "showButton", name = "Show sheathe button",
        description = "Show a movable button using the equipped weapon's icon", position = 1)
    default boolean showButton() { return true; }

    @Range(min = 24, max = 96)
    @ConfigItem(keyName = "buttonSize", name = "Button size",
        description = "Size of the sheathe/unsheathe button", position = 2)
    default int buttonSize() { return 44; }

    @ConfigSection(name = "Two-handed weapon placement",
        description = "Shared placement for two-handed weapons", position = 10)
    String axeSection = "axeSection";

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "twoHandedSideways", name = "Sideways", description = "Move left or right",
        section = axeSection, position = 11)
    default int sideways() { return -5; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "twoHandedHeight", name = "Height", description = "Move up or down",
        section = axeSection, position = 12)
    default int height() { return 142; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "twoHandedForward", name = "Forward", description = "Move toward or away from the back",
        section = axeSection, position = 13)
    default int forward() { return 15; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "twoHandedPitch", name = "Pitch", description = "Rotate forward/backward",
        section = axeSection, position = 14)
    default int pitch() { return -95; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "twoHandedYaw", name = "Yaw", description = "Turn around the player",
        section = axeSection, position = 15)
    default int yaw() { return 90; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "twoHandedRoll", name = "Roll", description = "Set the diagonal angle across the back",
        section = axeSection, position = 16)
    default int roll() { return -41; }

    @Range(min = 25, max = 200)
    @ConfigItem(keyName = "twoHandedScale", name = "Scale", description = "Weapon size percentage",
        section = axeSection, position = 17)
    default int scale() { return 100; }

    @ConfigSection(name = "Staff placement",
        description = "Shared placement for staffs, tridents, sceptres and wands", position = 20)
    String shadowSection = "shadowSection";

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "staffSideways", name = "Sideways", description = "Move left or right",
        section = shadowSection, position = 21)
    default int shadowSideways() { return 2; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "staffHeight", name = "Height", description = "Move up or down",
        section = shadowSection, position = 22)
    default int shadowHeight() { return 148; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "staffForward", name = "Forward", description = "Move toward or away from the back",
        section = shadowSection, position = 23)
    default int shadowForward() { return 15; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "staffPitch", name = "Pitch", description = "Rotate forward/backward",
        section = shadowSection, position = 24)
    default int shadowPitch() { return -77; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "staffYaw", name = "Yaw", description = "Turn around the player",
        section = shadowSection, position = 25)
    default int shadowYaw() { return 90; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "staffRoll", name = "Roll", description = "Set the diagonal angle across the back",
        section = shadowSection, position = 26)
    default int shadowRoll() { return -41; }

    @Range(min = 25, max = 200)
    @ConfigItem(keyName = "staffScale", name = "Scale", description = "Weapon size percentage",
        section = shadowSection, position = 27)
    default int shadowScale() { return 100; }

    @ConfigSection(name = "Bow placement",
        description = "Shared placement for bows", position = 30)
    String twistedBowSection = "twistedBowSection";

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "bowSideways", name = "Sideways", description = "Move left or right",
        section = twistedBowSection, position = 31)
    default int twistedBowSideways() { return -1; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "bowHeight", name = "Height", description = "Move up or down",
        section = twistedBowSection, position = 32)
    default int twistedBowHeight() { return 149; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "bowForward", name = "Forward", description = "Move toward or away from the back",
        section = twistedBowSection, position = 33)
    default int twistedBowForward() { return 24; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "bowPitch", name = "Pitch", description = "Rotate forward/backward",
        section = twistedBowSection, position = 34)
    default int twistedBowPitch() { return -98; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "bowYaw", name = "Yaw", description = "Turn around the player",
        section = twistedBowSection, position = 35)
    default int twistedBowYaw() { return 90; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "bowRoll", name = "Roll", description = "Set the diagonal angle across the back",
        section = twistedBowSection, position = 36)
    default int twistedBowRoll() { return -41; }

    @Range(min = 25, max = 200)
    @ConfigItem(keyName = "bowScale", name = "Scale", description = "Weapon size percentage",
        section = twistedBowSection, position = 37)
    default int twistedBowScale() { return 100; }
    @ConfigItem(keyName = "itemPlacements", name = "Individual weapon overrides", hidden = true,
        description = "itemID:sideways,height,forward,pitch,yaw,roll,scale; separate weapons with semicolons", position = 40)
    default String itemPlacements() { return ""; }
}
