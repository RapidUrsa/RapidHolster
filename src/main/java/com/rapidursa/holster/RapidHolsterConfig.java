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
        description = "Shared placement for staffs, tridents and sceptres", position = 20)
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
    @ConfigSection(name = "One-handed weapon placement",
        description = "One-handed melee weapons on the left hip", position = 50)
    String oneHandSection = "oneHandSection";

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "oneHandLeftSideways", name = "Sideways", description = "Adjust sideways placement",
        section = oneHandSection, position = 51)
    default int oneHandSideways() { return 24; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "oneHandHeight", name = "Height", description = "Adjust height placement",
        section = oneHandSection, position = 52)
    default int oneHandHeight() { return 90; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "oneHandForward", name = "Forward", description = "Adjust forward placement",
        section = oneHandSection, position = 53)
    default int oneHandForward() { return 12; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "oneHandPitch", name = "Pitch", description = "Adjust pitch placement",
        section = oneHandSection, position = 54)
    default int oneHandPitch() { return 32; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "oneHandYaw", name = "Yaw", description = "Adjust yaw placement",
        section = oneHandSection, position = 55)
    default int oneHandYaw() { return 180; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "oneHandRoll", name = "Roll", description = "Adjust roll placement",
        section = oneHandSection, position = 56)
    default int oneHandRoll() { return 0; }

    @Range(min = 25, max = 200)
    @ConfigItem(keyName = "oneHandScale", name = "Scale", description = "Adjust scale placement",
        section = oneHandSection, position = 57)
    default int oneHandScale() { return 100; }

    @ConfigSection(name = "Wand placement",
        description = "Wands on the left hip", position = 60)
    String wandSection = "wandSection";

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "wandLeftSideways", name = "Sideways", description = "Adjust sideways placement",
        section = wandSection, position = 61)
    default int wandSideways() { return 24; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "wandHeight", name = "Height", description = "Adjust height placement",
        section = wandSection, position = 62)
    default int wandHeight() { return 90; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "wandForward", name = "Forward", description = "Adjust forward placement",
        section = wandSection, position = 63)
    default int wandForward() { return 12; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "wandPitch", name = "Pitch", description = "Adjust pitch placement",
        section = wandSection, position = 64)
    default int wandPitch() { return 32; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "wandYaw", name = "Yaw", description = "Adjust yaw placement",
        section = wandSection, position = 65)
    default int wandYaw() { return 180; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "wandRoll", name = "Roll", description = "Adjust roll placement",
        section = wandSection, position = 66)
    default int wandRoll() { return 0; }

    @Range(min = 25, max = 200)
    @ConfigItem(keyName = "wandScale", name = "Scale", description = "Adjust scale placement",
        section = wandSection, position = 67)
    default int wandScale() { return 100; }

    @ConfigSection(name = "Shield placement (back)",
        description = "Shields, wards and bucklers on the back", position = 70)
    String shieldSection = "shieldSection";

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "shieldSideways", name = "Sideways", description = "Adjust sideways placement",
        section = shieldSection, position = 71)
    default int shieldSideways() { return 0; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "shieldHeight", name = "Height", description = "Adjust height placement",
        section = shieldSection, position = 72)
    default int shieldHeight() { return 158; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "shieldForward", name = "Forward", description = "Adjust forward placement",
        section = shieldSection, position = 73)
    default int shieldForward() { return 22; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "shieldPitch", name = "Pitch", description = "Adjust pitch placement",
        section = shieldSection, position = 74)
    default int shieldPitch() { return 0; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "shieldYaw", name = "Yaw", description = "Adjust yaw placement",
        section = shieldSection, position = 75)
    default int shieldYaw() { return -88; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "shieldRoll", name = "Roll", description = "Adjust roll placement",
        section = shieldSection, position = 76)
    default int shieldRoll() { return 0; }

    @Range(min = 25, max = 200)
    @ConfigItem(keyName = "shieldScale", name = "Scale", description = "Adjust scale placement",
        section = shieldSection, position = 77)
    default int shieldScale() { return 85; }

    @ConfigSection(name = "Crossbow placement",
        description = "Shared crossbow placement", position = 80)
    String crossbowSection = "crossbowSection";

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "crossbowLeftSideways", name = "Sideways", description = "Adjust sideways placement",
        section = crossbowSection, position = 81)
    default int crossbowSideways() { return -13; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "crossbowHeight", name = "Height", description = "Adjust height placement",
        section = crossbowSection, position = 82)
    default int crossbowHeight() { return 166; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "crossbowForward", name = "Forward", description = "Adjust forward placement",
        section = crossbowSection, position = 83)
    default int crossbowForward() { return 22; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "crossbowPitch", name = "Pitch", description = "Adjust pitch placement",
        section = crossbowSection, position = 84)
    default int crossbowPitch() { return 2; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "crossbowYaw", name = "Yaw", description = "Adjust yaw placement",
        section = crossbowSection, position = 85)
    default int crossbowYaw() { return 180; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "crossbowRoll", name = "Roll", description = "Adjust roll placement",
        section = crossbowSection, position = 86)
    default int crossbowRoll() { return -33; }

    @Range(min = 25, max = 200)
    @ConfigItem(keyName = "crossbowScale", name = "Scale", description = "Adjust scale placement",
        section = crossbowSection, position = 87)
    default int crossbowScale() { return 100; }

    @ConfigSection(name = "Off-hand placement (right hip)",
        description = "Books, defenders and other non-shield off-hand items", position = 90)
    String offHandHipSection = "offHandHipSection";

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "offHandHipSideways", name = "Sideways", description = "Move left or right",
        section = offHandHipSection, position = 91)
    default int offHandHipSideways() { return -26; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "offHandHipHeight", name = "Height", description = "Move up or down",
        section = offHandHipSection, position = 92)
    default int offHandHipHeight() { return 90; }

    @Range(min = -200, max = 200)
    @ConfigItem(keyName = "offHandHipForward", name = "Forward", description = "Move forward or back",
        section = offHandHipSection, position = 93)
    default int offHandHipForward() { return 10; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "offHandHipPitch", name = "Pitch", description = "Rotate up or down",
        section = offHandHipSection, position = 94)
    default int offHandHipPitch() { return 48; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "offHandHipYaw", name = "Yaw", description = "Turn around the player",
        section = offHandHipSection, position = 95)
    default int offHandHipYaw() { return 174; }

    @Range(min = -180, max = 180)
    @ConfigItem(keyName = "offHandHipRoll", name = "Roll", description = "Set the hip angle",
        section = offHandHipSection, position = 96)
    default int offHandHipRoll() { return 0; }

    @Range(min = 25, max = 200)
    @ConfigItem(keyName = "offHandHipScale", name = "Scale", description = "Item size percentage",
        section = offHandHipSection, position = 97)
    default int offHandHipScale() { return 100; }

    @ConfigItem(keyName = "itemPlacements", name = "Individual weapon overrides", hidden = true,
        description = "itemID:sideways,height,forward,pitch,yaw,roll,scale; separate weapons with semicolons", position = 40)
    default String itemPlacements() { return ""; }
}
