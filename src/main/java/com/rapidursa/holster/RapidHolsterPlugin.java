package com.rapidursa.holster;

import com.google.inject.Provides;
import com.rapidursa.holster.appearance.ModelRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerChanged;
import net.runelite.api.kit.KitType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
    name = "Rapid Holster",
    description = "Visually holster equipped weapons and off-hand items on your character",
    tags = {"weapon", "shield", "offhand", "holster", "sheathe", "cosmetic", "fashionscape"}
)
public class RapidHolsterPlugin extends Plugin
{
    public static final String CONFIG_GROUP = "rapidholster";
    // The live, assembled Soulreaper axe is the later 28338 item variant.
    private static final int SOULREAPER_AXE = ItemID.SOULREAPER_AXE_28338;
    private static final int TUMEKENS_SHADOW = ItemID.TUMEKENS_SHADOW;
    private static final int TUMEKENS_SHADOW_UNCHARGED = ItemID.TUMEKENS_SHADOW_UNCHARGED;
    private static final int TWISTED_BOW = ItemID.TWISTED_BOW;
    private static final int ITEM_OFFSET = PlayerComposition.ITEM_OFFSET;
    private static final int WEAPON_SLOT = KitType.WEAPON.getIndex();
    private static final int SHIELD_SLOT = KitType.SHIELD.getIndex();
    private static final int CAPE_SLOT = KitType.CAPE.getIndex();
    private static final int AMBIENT = 64;
    private static final int CONTRAST = 768;
    private static final int LIGHT_X = -50;
    private static final int LIGHT_Y = -10;
    private static final int LIGHT_Z = -50;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private RapidHolsterConfig config;
    @Inject private ConfigManager configManager;
    @Inject private ModelRepository modelRepository;
    @Inject private ItemManager itemManager;
    @Inject private OverlayManager overlayManager;
    @Inject private MouseManager mouseManager;
    @Inject private HolsterToggleOverlay holsterButton;
    @Inject private CustomWeaponPanel customPanel;
    @Inject private ClientToolbar toolbar;
    private NavigationButton customNavigation;

    private RuneLiteObject holsteredWeapon;
    private RuneLiteObject holsteredShield;
    private int realWeaponKit;
    private PlayerComposition hiddenWeaponComposition;
    private int realShieldKit;
    private PlayerComposition hiddenShieldComposition;
    private int realCapeKit;
    private PlayerComposition hiddenCapeComposition;
    private boolean applyingAppearance;
    private boolean rebuildModel = true;
    private final int[] naturalPose = new int[8];
    private boolean poseCaptured;
    private int combatDrawTicks;
    private Model holsteredModel;
    private float[] baseWeaponX;
    private float[] baseWeaponY;
    private float[] baseWeaponZ;
    private Model holsteredShieldModel;
    private float[] baseShieldX;
    private float[] baseShieldY;
    private float[] baseShieldZ;
    private int builtShieldItemId = -1;
    private TorsoRig torsoRig;
    private boolean torsoRigCapturedWhileIdle;
    private int builtWeaponItemId = -1;
    private WeaponGroup weaponGroup = WeaponGroup.NONE;
    private enum WeaponGroup { NONE, STAFF, BOW, CROSSBOW, TWO_HANDED, ONE_HANDED, WAND }

    @Provides
    RapidHolsterConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RapidHolsterConfig.class);
    }

    @Override
    protected void startUp()
    {
        updateMountedHandoffSupport();
        customPanel.bind(this);
        customNavigation = NavigationButton.builder().tooltip("Rapid Holster")
            .icon(CustomWeaponPanel.icon()).priority(8).panel(customPanel).build();
        toolbar.addNavigation(customNavigation);
        holsterButton.bind(this);
        overlayManager.add(holsterButton);
        mouseManager.registerMouseListener(holsterButton);
        clientThread.invokeLater(this::refresh);
    }

    @Override
    protected void shutDown()
    {
        configManager.unsetConfiguration(CONFIG_GROUP, "mountedHandoffSupported");
        customPanel.unbind();
        if (customNavigation != null) toolbar.removeNavigation(customNavigation);
        customNavigation = null;
        mouseManager.unregisterMouseListener(holsterButton);
        overlayManager.remove(holsterButton);
        holsterButton.unbind();
        clientThread.invoke(() -> {
            restoreCape();
            restoreHeldWeapon();
            restoreShield();
            destroyObject();
            destroyShield();
            modelRepository.unload();
            restoreNaturalPose();
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            rebuildModel = true;
            clientThread.invokeLater(this::refresh);
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN
            || event.getGameState() == GameState.HOPPING)
        {
            destroyObject();
            destroyShield();
            realWeaponKit = 0;
            hiddenWeaponComposition = null;
            restoreShield();
            realShieldKit = 0;
            restoreCape();
            weaponGroup = WeaponGroup.NONE;
            combatDrawTicks = 0;
            poseCaptured = false;
        }
    }

    @Subscribe(priority = 1)
    public void onPlayerChanged(PlayerChanged event)
    {
        if (event.getPlayer() != client.getLocalPlayer() || applyingAppearance)
        {
            return;
        }
        captureRealWeapon();
        refresh();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        updateMountedHandoffSupport();
        if (combatDrawTicks > 0)
        {
            combatDrawTicks--;
        }
        refresh();
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event)
    {
        if (mountedRiderOwnsHolsters())
        {
            destroyObject();
            destroyShield();
            return;
        }
        Player player = client.getLocalPlayer();
        if (player == null || (holsteredWeapon == null || !holsteredWeapon.isActive())
            && (holsteredShield == null || !holsteredShield.isActive()))
        {
            return;
        }
        positionOnAnimatedTorso(player);
        if (holsteredWeapon != null && holsteredWeapon.isActive())
            applyUnarmedPose(player);
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (event.getActor() != client.getLocalPlayer())
        {
            return;
        }
        int animation = event.getActor().getAnimation();
        if (animation != -1 && isSupportedWeapon(itemId(realWeaponKit)))
        {
            // Keep the real weapon drawn across the complete attack cycle and the
            // short delay between repeated attacks.
            combatDrawTicks = drawHoldTicks();
            refresh();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if ("rapidursamounts".equals(event.getGroup())
            && "mountedHolsterActive".equals(event.getKey()))
        {
            if (!mountedRiderOwnsHolsters()) clientThread.invokeLater(this::refresh);
            return;
        }
        if (!CONFIG_GROUP.equals(event.getGroup()))
        {
            return;
        }
        // This heartbeat changes every tick. It is not a placement setting;
        // rebuilding here continually resets the torso attachment while running.
        if ("mountedHandoffSupported".equals(event.getKey()))
        {
            return;
        }
        rebuildModel = true;
        customPanel.syncHideCape();
        clientThread.invokeLater(this::refresh);
    }

    private void updateMountedHandoffSupport()
    {
        if (client.getGameState() == GameState.LOGGED_IN && config.holstered())
            configManager.setConfiguration(CONFIG_GROUP, "mountedHandoffSupported", System.currentTimeMillis());
        else
            configManager.unsetConfiguration(CONFIG_GROUP, "mountedHandoffSupported");
    }

    private boolean mountedRiderOwnsHolsters()
    {
        String timestamp = configManager.getConfiguration("rapidursamounts", "mountedHolsterActive");
        if (timestamp == null) return false;
        try
        {
            long age = System.currentTimeMillis() - Long.parseLong(timestamp);
            return age >= 0 && age < 2000;
        }
        catch (NumberFormatException ignored)
        {
            return false;
        }
    }

    private void refresh()
    {
        Player player = client.getLocalPlayer();
        if (client.getGameState() != GameState.LOGGED_IN || player == null
            || player.getPlayerComposition() == null)
        {
            return;
        }

        if (mountedRiderOwnsHolsters())
        {
            restoreHeldWeapon();
            restoreShield();
            restoreNaturalPose();
            destroyObject();
            destroyShield();
            return;
        }

        updateCape(player.getPlayerComposition());
        captureRealWeapon();
        captureRealShield();
        int weaponItemId = itemId(realWeaponKit);
        int shieldItemId = itemId(realShieldKit);
        WeaponGroup detected = detectWeaponGroup(weaponItemId);
        if (detected == WeaponGroup.NONE && itemOverride(weaponItemId) != null)
            detected = WeaponGroup.TWO_HANDED;
        if (detected != weaponGroup) rebuildModel = true;
        weaponGroup = detected;
        boolean supported = isSupportedWeapon(weaponItemId);
        if (!config.holstered() || (!supported && shieldItemId < 0))
        {
            restoreHeldWeapon();
            restoreShield();
            restoreNaturalPose();
            destroyObject();
            destroyShield();
            return;
        }
        if (combatDrawTicks > 0 || player.getAnimation() != -1)
        {
            restoreHeldWeapon();
            restoreShield();
            restoreNaturalPose();
            if (holsteredWeapon != null)
            {
                holsteredWeapon.setActive(false);
            }
            if (holsteredShield != null) holsteredShield.setActive(false);
            return;
        }

        ensureModelRepository();
        boolean updateModels = rebuildModel;
        if (supported && (holsteredWeapon == null || updateModels || builtWeaponItemId != weaponItemId))
        {
            rebuildObject(weaponItemId);
        }
        if (shieldItemId >= 0 && (holsteredShield == null || updateModels || builtShieldItemId != shieldItemId))
        {
            rebuildShield(shieldItemId);
        }
        if (!supported) { restoreHeldWeapon(); destroyObject(); }
        if (shieldItemId < 0) { restoreShield(); destroyShield(); }
        rebuildModel = false;
        if (supported && holsteredWeapon == null) restoreHeldWeapon();
        if (shieldItemId >= 0 && holsteredShield == null) restoreShield();
        boolean weaponReady = supported && holsteredWeapon != null;
        boolean shieldReady = shieldItemId >= 0 && holsteredShield != null;
        if (!weaponReady && !shieldReady)
        {
            restoreNaturalPose();
            return;
        }
        if (weaponReady)
        {
            captureNaturalPose(player);
            hideHeldWeapon();
            applyUnarmedPose(player);
        }
        else restoreNaturalPose();
        if (shieldReady) hideShield();
        positionOnAnimatedTorso(player);
        if (weaponReady && !holsteredWeapon.isActive()) holsteredWeapon.setActive(true);
        if (shieldReady && !holsteredShield.isActive()) holsteredShield.setActive(true);
    }

    private void captureRealWeapon()
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getPlayerComposition() == null)
        {
            return;
        }
        PlayerComposition composition = player.getPlayerComposition();
        int kit = composition.getEquipmentIds()[WEAPON_SLOT];
        // After a mounted handoff, the player's appearance can still contain
        // Holster's temporary empty slot. The actual equipment is authoritative
        // unless this composition is the one we deliberately hid ourselves.
        if (kit <= ITEM_OFFSET && composition != hiddenWeaponComposition)
        {
            int equipped = wornItemId(WEAPON_SLOT);
            if (equipped >= 0) kit = equipped + ITEM_OFFSET;
        }
        if (kit != ITEM_OFFSET || composition != hiddenWeaponComposition)
        {
            if (kit != realWeaponKit) rebuildModel = true;
            realWeaponKit = kit;
            hiddenWeaponComposition = null;
        }
    }

    private static int itemId(int kit)
    {
        return kit >= ITEM_OFFSET ? kit - ITEM_OFFSET : -1;
    }

    private int wornItemId(int slot)
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null || equipment.getItems().length <= slot
            || equipment.getItems()[slot] == null) return -1;
        return equipment.getItems()[slot].getId();
    }

    private void hideHeldWeapon()
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getPlayerComposition() == null) return;
        hiddenWeaponComposition = player.getPlayerComposition();
        setWeaponKit(ITEM_OFFSET);
    }

    private void restoreHeldWeapon()
    {
        Player player = client.getLocalPlayer();
        if (hiddenWeaponComposition != null && player != null
            && player.getPlayerComposition() == hiddenWeaponComposition
            && hiddenWeaponComposition.getEquipmentIds()[WEAPON_SLOT] == ITEM_OFFSET)
            setWeaponKit(realWeaponKit);
        hiddenWeaponComposition = null;
    }

    private void setWeaponKit(int kit)
    {
        setEquipmentKit(WEAPON_SLOT, kit);
    }

    private void captureRealShield()
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getPlayerComposition() == null) return;
        PlayerComposition composition = player.getPlayerComposition();
        int kit = composition.getEquipmentIds()[SHIELD_SLOT];
        if (kit <= ITEM_OFFSET && composition != hiddenShieldComposition)
        {
            int equipped = wornItemId(SHIELD_SLOT);
            if (equipped >= 0) kit = equipped + ITEM_OFFSET;
        }
        if (kit != ITEM_OFFSET || composition != hiddenShieldComposition)
        {
            if (kit != realShieldKit) rebuildModel = true;
            realShieldKit = kit;
            hiddenShieldComposition = null;
        }
    }

    private void hideShield()
    {
        if (realShieldKit < ITEM_OFFSET) return;
        Player player = client.getLocalPlayer();
        if (player == null || player.getPlayerComposition() == null) return;
        hiddenShieldComposition = player.getPlayerComposition();
        setEquipmentKit(SHIELD_SLOT, ITEM_OFFSET);
    }

    private void restoreShield()
    {
        Player player = client.getLocalPlayer();
        if (hiddenShieldComposition != null && player != null
            && player.getPlayerComposition() == hiddenShieldComposition
            && hiddenShieldComposition.getEquipmentIds()[SHIELD_SLOT] == ITEM_OFFSET)
            setEquipmentKit(SHIELD_SLOT, realShieldKit);
        hiddenShieldComposition = null;
    }

    private void updateCape(PlayerComposition composition)
    {
        if (!config.hideCape())
        {
            restoreCape();
            return;
        }
        int kit = composition.getEquipmentIds()[CAPE_SLOT];
        // A server appearance update replaces our sentinel with the new cape
        // (or zero when unequipped). Capture it before hiding it again.
        if (composition != hiddenCapeComposition || kit != ITEM_OFFSET)
        {
            realCapeKit = kit;
            hiddenCapeComposition = composition;
        }
        setEquipmentKit(CAPE_SLOT, ITEM_OFFSET);
    }

    private void restoreCape()
    {
        Player player = client.getLocalPlayer();
        if (hiddenCapeComposition != null && player != null
            && player.getPlayerComposition() == hiddenCapeComposition
            && hiddenCapeComposition.getEquipmentIds()[CAPE_SLOT] == ITEM_OFFSET)
        {
            setEquipmentKit(CAPE_SLOT, realCapeKit);
        }
        hiddenCapeComposition = null;
    }

    private void setEquipmentKit(int slot, int kit)
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getPlayerComposition() == null)
        {
            return;
        }
        int[] equipment = player.getPlayerComposition().getEquipmentIds();
        if (equipment[slot] == kit)
        {
            return;
        }
        applyingAppearance = true;
        try
        {
            equipment[slot] = kit;
            player.getPlayerComposition().setHash();
        }
        finally
        {
            applyingAppearance = false;
        }
    }

    private void captureNaturalPose(Player player)
    {
        if (poseCaptured)
        {
            return;
        }
        naturalPose[0] = player.getIdlePoseAnimation();
        naturalPose[1] = player.getIdleRotateLeft();
        naturalPose[2] = player.getIdleRotateRight();
        naturalPose[3] = player.getWalkAnimation();
        naturalPose[4] = player.getWalkRotate180();
        naturalPose[5] = player.getWalkRotateLeft();
        naturalPose[6] = player.getWalkRotateRight();
        naturalPose[7] = player.getRunAnimation();
        poseCaptured = true;
    }

    private static void applyUnarmedPose(Player player)
    {
        player.setIdlePoseAnimation(808);
        player.setIdleRotateLeft(823);
        player.setIdleRotateRight(823);
        player.setWalkAnimation(819);
        player.setWalkRotate180(820);
        player.setWalkRotateLeft(821);
        player.setWalkRotateRight(822);
        player.setRunAnimation(824);
    }

    private void restoreNaturalPose()
    {
        if (!poseCaptured)
        {
            return;
        }
        Player player = client.getLocalPlayer();
        if (player != null)
        {
            player.setIdlePoseAnimation(naturalPose[0]);
            player.setIdleRotateLeft(naturalPose[1]);
            player.setIdleRotateRight(naturalPose[2]);
            player.setWalkAnimation(naturalPose[3]);
            player.setWalkRotate180(naturalPose[4]);
            player.setWalkRotateLeft(naturalPose[5]);
            player.setWalkRotateRight(naturalPose[6]);
            player.setRunAnimation(naturalPose[7]);
        }
        poseCaptured = false;
    }

    private void ensureModelRepository()
    {
        if (!modelRepository.isLoaded())
        {
            modelRepository.loadFromClient(client);
        }
    }

    private void rebuildObject(int weaponItemId)
    {
        destroyObject();
        ModelData data = buildItemModel(weaponItemId, placementFor(weaponItemId));
        if (data == null)
        {
            log.debug("Wearable model for item {} is not available yet", weaponItemId);
            return;
        }
        builtWeaponItemId = weaponItemId;
        holsteredWeapon = client.createRuneLiteObject();
        holsteredModel = data.light(AMBIENT, CONTRAST, LIGHT_X, LIGHT_Y, LIGHT_Z);
        baseWeaponX = holsteredModel.getVerticesX().clone();
        baseWeaponY = holsteredModel.getVerticesY().clone();
        baseWeaponZ = holsteredModel.getVerticesZ().clone();
        holsteredWeapon.setModel(holsteredModel);
        holsteredWeapon.setRenderMode(Renderable.RENDERMODE_SORTED_NO_DEPTH);
        holsteredWeapon.setDrawFrontTilesFirst(true);
        holsteredWeapon.setActive(true);
    }

    private void rebuildShield(int shieldItemId)
    {
        destroyShield();
        ModelData data = buildItemModel(shieldItemId, placementForOffHand(shieldItemId));
        if (data == null)
        {
            log.debug("Wearable shield model for item {} is not available yet", shieldItemId);
            return;
        }
        builtShieldItemId = shieldItemId;
        holsteredShield = client.createRuneLiteObject();
        holsteredShieldModel = data.light(AMBIENT, CONTRAST, LIGHT_X, LIGHT_Y, LIGHT_Z);
        baseShieldX = holsteredShieldModel.getVerticesX().clone();
        baseShieldY = holsteredShieldModel.getVerticesY().clone();
        baseShieldZ = holsteredShieldModel.getVerticesZ().clone();
        holsteredShield.setModel(holsteredShieldModel);
        holsteredShield.setRenderMode(Renderable.RENDERMODE_SORTED_NO_DEPTH);
        holsteredShield.setDrawFrontTilesFirst(true);
        holsteredShield.setActive(true);
    }

    private ModelData buildItemModel(int itemId, Placement placement)
    {
        ModelRepository.Entry entry = modelRepository.item(itemId);
        Player player = client.getLocalPlayer();
        if (entry == null || player == null || player.getPlayerComposition() == null)
        {
            return null;
        }
        int[] ids = entry.models(player.getPlayerComposition().getGender());
        if (ids == null)
        {
            return null;
        }

        List<ModelData> parts = new ArrayList<>();
        for (int id : ids)
        {
            if (id < 0) continue;
            ModelData part = client.loadModelData(id);
            if (part == null) continue;
            if (entry.cf != null && entry.cr != null)
            {
                part = part.cloneColors();
                for (int i = 0; i < Math.min(entry.cf.length, entry.cr.length); i++)
                    part.recolor(entry.cf[i], entry.cr[i]);
            }
            if (entry.tf != null && entry.tr != null)
            {
                part = part.cloneTextures();
                for (int i = 0; i < Math.min(entry.tf.length, entry.tr.length); i++)
                    part.retexture(entry.tf[i], entry.tr[i]);
            }
            parts.add(part);
        }
        if (parts.isEmpty()) return null;
        ModelData weapon = parts.size() == 1 ? parts.get(0)
            : client.mergeModels(parts.toArray(new ModelData[0]));
        if (weapon == null) return null;
        weapon = weapon.cloneVertices();
        transform(weapon, placement);
        return weapon;
    }

    private void transform(ModelData data, Placement placement)
    {
        float[] x = data.getVerticesX();
        float[] y = data.getVerticesY();
        float[] z = data.getVerticesZ();
        int count = data.getVerticesCount();
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++)
        {
            minX = Math.min(minX, x[i]); maxX = Math.max(maxX, x[i]);
            minY = Math.min(minY, y[i]); maxY = Math.max(maxY, y[i]);
            minZ = Math.min(minZ, z[i]); maxZ = Math.max(maxZ, z[i]);
        }
        float cx = (minX + maxX) * .5f;
        float cy = (minY + maxY) * .5f;
        float cz = (minZ + maxZ) * .5f;
        int sideways = placement.sideways;
        int height = placement.height;
        int forward = placement.forward;
        int scalePercent = placement.scale;
        double pitch = Math.toRadians(placement.pitch);
        double yaw = Math.toRadians(placement.yaw);
        double roll = Math.toRadians(placement.roll);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double cyaw = Math.cos(yaw), syaw = Math.sin(yaw);
        double cr = Math.cos(roll), sr = Math.sin(roll);
        double size = scalePercent / 100.0;

        for (int i = 0; i < count; i++)
        {
            double vx = (x[i] - cx) * size;
            double vy = (y[i] - cy) * size;
            double vz = (z[i] - cz) * size;
            double py = vy * cp - vz * sp;
            double pz = vy * sp + vz * cp;
            double yx = vx * cyaw + pz * syaw;
            double yz = -vx * syaw + pz * cyaw;
            double rx = yx * cr - py * sr;
            double ry = yx * sr + py * cr;
            x[i] = (float) rx + sideways;
            y[i] = (float) ry - height;
            z[i] = (float) yz + forward;
        }
    }

    int getEquippedWeaponItemId()
    {
        return itemId(realWeaponKit);
    }

    int getEquippedShieldItemId()
    {
        return itemId(realShieldKit);
    }

    boolean isHolstered()
    {
        return config.holstered() && combatDrawTicks == 0
            && (holsteredWeapon != null && holsteredWeapon.isActive()
                || holsteredShield != null && holsteredShield.isActive());
    }

    void toggleHolstered()
    {
        configManager.setConfiguration(CONFIG_GROUP, "holstered", !config.holstered());
        refresh();
    }

    boolean isSupportedWeapon(int itemId)
    {
        return itemId >= 0 && itemId == getEquippedWeaponItemId()
            && weaponGroup != WeaponGroup.NONE;
    }

    boolean hasHolsterableGear()
    {
        return isSupportedWeapon(getEquippedWeaponItemId()) || getEquippedShieldItemId() >= 0;
    }

    private static boolean isShadow(int itemId)
    {
        return itemId == TUMEKENS_SHADOW || itemId == TUMEKENS_SHADOW_UNCHARGED;
    }

    private int drawHoldTicks()
    {
        ItemStats stats = itemManager.getItemStats(getEquippedWeaponItemId());
        if (stats != null && stats.getEquipment() != null)
            return Math.max(5, stats.getEquipment().getAspeed() + 1);
        return 7;
    }

    private WeaponGroup detectWeaponGroup(int id)
    {
        if (id < 0) return WeaponGroup.NONE;
        if (isShadow(id)) return WeaponGroup.STAFF;
        if (id == TWISTED_BOW) return WeaponGroup.BOW;
        if (id == SOULREAPER_AXE) return WeaponGroup.TWO_HANDED;
        String name = itemManager.getItemComposition(id).getName().toLowerCase(Locale.ROOT);
        // Crossbows need their own hip preset even when equipment metadata
        // describes the crossbow as two-handed.
        if (name.contains("crossbow")) return WeaponGroup.CROSSBOW;
        if (name.contains("wand")) return WeaponGroup.WAND;
        if (name.contains("staff") || name.contains("trident") || name.contains("sceptre")
            || name.contains("scepter") || name.contains("eye of ayak"))
            return WeaponGroup.STAFF;
        if (name.contains("bow")) return WeaponGroup.BOW;
        ItemStats stats = itemManager.getItemStats(id);
        if (stats != null && stats.getEquipment() != null && stats.getEquipment().isTwoHanded())
            return WeaponGroup.TWO_HANDED;
        if (stats != null && stats.getEquipment() != null && stats.getEquipment().getSlot() == 3)
            return WeaponGroup.ONE_HANDED;
        return WeaponGroup.NONE;
    }

    private Placement itemOverride(int itemId)
    {
        for (String entry : config.itemPlacements().split(";"))
        {
            String[] parts = entry.trim().split(":");
            if (parts.length != 2) continue;
            try
            {
                if (Integer.parseInt(parts[0].trim()) != itemId) continue;
                String[] values = parts[1].split(",");
                if (values.length != 7) continue;
                int[] v = new int[7];
                for (int i = 0; i < 7; i++) v[i] = Integer.parseInt(values[i].trim());
                if (v[6] < 25 || v[6] > 200) continue;
                boolean valid = true;
                for (int i = 0; i < 6; i++)
                    if (v[i] < (i < 3 ? -200 : -180) || v[i] > (i < 3 ? 200 : 180)) valid = false;
                if (valid) return new Placement(v[0], v[1], v[2], v[3], v[4], v[5], v[6]);
            }
            catch (NumberFormatException ignored) { /* Use the category preset. */ }
        }
        return null;
    }

    private Placement placementFor(int itemId)
    {
        Placement override = itemOverride(itemId);
        if (override != null) return override;
        return categoryPlacement(itemId);
    }

    private Placement placementForOffHand(int itemId)
    {
        Placement override = itemOverride(itemId);
        return override != null ? override : offHandPlacement(itemId);
    }

    int[] defaultPlacement(int itemId)
    {
        ItemStats stats = itemManager.getItemStats(itemId);
        boolean offHand = itemId == getEquippedShieldItemId()
            || stats != null && stats.getEquipment() != null && stats.getEquipment().getSlot() == 5;
        Placement p = offHand ? offHandPlacement(itemId) : categoryPlacement(itemId);
        return new int[]{p.sideways, p.height, p.forward, p.pitch, p.yaw, p.roll, p.scale};
    }

    private Placement offHandPlacement(int itemId)
    {
        String name = itemManager.getItemComposition(itemId).getName().toLowerCase(Locale.ROOT);
        // Some shields have a game-specific name without "shield" in it.
        if (name.contains("shield") || name.contains("ward")
            || name.contains("buckler") || name.contains("toktz-ket-xil"))
            return shieldPlacement();
        return new Placement(config.offHandHipSideways(), config.offHandHipHeight(),
            config.offHandHipForward(), config.offHandHipPitch(), config.offHandHipYaw(),
            config.offHandHipRoll(), config.offHandHipScale());
    }

    private Placement shieldPlacement()
    {
        return new Placement(config.shieldSideways(), config.shieldHeight(),
            config.shieldForward(), config.shieldPitch(), config.shieldYaw(),
            config.shieldRoll(), config.shieldScale());
    }

    private Placement categoryPlacement(int itemId)
    {
        if (detectWeaponGroup(itemId) == WeaponGroup.CROSSBOW)
            return new Placement(config.crossbowSideways(), config.crossbowHeight(),
                config.crossbowForward(), config.crossbowPitch(), config.crossbowYaw(),
                config.crossbowRoll(), config.crossbowScale());
        if (detectWeaponGroup(itemId) == WeaponGroup.ONE_HANDED)
            return new Placement(config.oneHandSideways(), config.oneHandHeight(),
                config.oneHandForward(), config.oneHandPitch(), config.oneHandYaw(),
                config.oneHandRoll(), config.oneHandScale());
        if (detectWeaponGroup(itemId) == WeaponGroup.WAND)
            return new Placement(config.wandSideways(), config.wandHeight(),
                config.wandForward(), config.wandPitch(), config.wandYaw(),
                config.wandRoll(), config.wandScale());
        if (detectWeaponGroup(itemId) == WeaponGroup.STAFF)
        {
            return new Placement(config.shadowSideways(), config.shadowHeight(),
                config.shadowForward(), config.shadowPitch(), config.shadowYaw(),
                config.shadowRoll(), config.shadowScale());
        }
        if (detectWeaponGroup(itemId) == WeaponGroup.BOW)
        {
            return new Placement(config.twistedBowSideways(), config.twistedBowHeight(),
                config.twistedBowForward(), config.twistedBowPitch(), config.twistedBowYaw(),
                config.twistedBowRoll(), config.twistedBowScale());
        }
        return new Placement(config.sideways(), config.height(), config.forward(),
            config.pitch(), config.yaw(), config.roll(), config.scale());
    }

    private static final class Placement
    {
        private final int sideways;
        private final int height;
        private final int forward;
        private final int pitch;
        private final int yaw;
        private final int roll;
        private final int scale;

        private Placement(int sideways, int height, int forward, int pitch,
            int yaw, int roll, int scale)
        {
            this.sideways = sideways;
            this.height = height;
            this.forward = forward;
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
            this.scale = scale;
        }
    }

    /** RuneLite objects do not inherit an actor's animated skeleton. Track
     * upper torso vertices for run-cycle movement while keeping the fitted
     * weapon angle steady as the player turns.
     */
    private void positionOnAnimatedTorso(Player player)
    {
        LocalPoint playerPoint = player.getLocalLocation();
        Model playerModel = player.getModel();
        if (playerPoint == null || playerModel == null
            || holsteredWeapon == null && holsteredShield == null)
        {
            return;
        }

        boolean idle = player.getPoseAnimation() == player.getIdlePoseAnimation()
            && player.getAnimation() == -1;
        if (torsoRig == null || torsoRig.vertexCount != playerModel.getVerticesCount()
            || (idle && !torsoRigCapturedWhileIdle))
        {
            torsoRig = null;
            TorsoRig candidate = TorsoRig.capture(playerModel);
            if (candidate != null)
            {
                torsoRig = candidate;
                torsoRigCapturedWhileIdle = idle;
            }
        }

        int orientation = player.getCurrentOrientation();
        int plane = player.getWorldLocation().getPlane();
        if (holsteredWeapon != null)
            positionItem(playerModel, holsteredWeapon, holsteredModel, baseWeaponX,
                baseWeaponY, baseWeaponZ, playerPoint, plane, orientation);
        if (holsteredShield != null)
            positionItem(playerModel, holsteredShield, holsteredShieldModel, baseShieldX,
                baseShieldY, baseShieldZ, playerPoint, plane, orientation);
    }

    private void positionItem(Model playerModel, RuneLiteObject object, Model itemModel,
        float[] baseX, float[] baseY, float[] baseZ, LocalPoint location,
        int plane, int orientation)
    {
        if (torsoRig != null && itemModel != null && baseX != null)
        {
            torsoRig.apply(playerModel, itemModel, baseX, baseY, baseZ);
            itemModel.calculateBoundsCylinder();
            object.setModel(itemModel);
        }
        object.setLocation(location, plane);
        object.setZ(Perspective.getTileHeight(client, location, plane));
        object.setOrientation(orientation);
    }

    private static final class TorsoRig
    {
        private final int vertexCount;
        private final int[] torso;
        private final int[] left;
        private final int[] right;
        private final int[] top;
        private final int[] bottom;
        private final Vec3 referenceAnchor;
        private final Frame referenceFrame;

        private TorsoRig(int vertexCount, int[] torso, int[] left, int[] right,
            int[] top, int[] bottom, Vec3 referenceAnchor, Frame referenceFrame)
        {
            this.vertexCount = vertexCount;
            this.torso = torso;
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
            this.referenceAnchor = referenceAnchor;
            this.referenceFrame = referenceFrame;
        }

        private static TorsoRig capture(Model model)
        {
            int count = model.getVerticesCount();
            if (count == 0)
            {
                return null;
            }
            float[] x = model.getVerticesX();
            float[] y = model.getVerticesY();
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (int i = 0; i < count; i++)
            {
                minX = Math.min(minX, x[i]); maxX = Math.max(maxX, x[i]);
                minY = Math.min(minY, y[i]); maxY = Math.max(maxY, y[i]);
            }

            double centreX = (minX + maxX) * .5;
            double halfWidth = Math.max(12.0, (maxX - minX) * .28);
            double upperStart = minY + (maxY - minY) * .16;
            double upperEnd = minY + (maxY - minY) * .56;
            List<Integer> torsoList = new ArrayList<>();
            for (int i = 0; i < count; i++)
            {
                if (y[i] >= upperStart && y[i] <= upperEnd
                    && Math.abs(x[i] - centreX) <= halfWidth)
                {
                    torsoList.add(i);
                }
            }
            if (torsoList.size() < 12)
            {
                return null;
            }

            int[] torso = toArray(torsoList);
            Vec3 anchor = average(model, torso);
            List<Integer> leftList = new ArrayList<>();
            List<Integer> rightList = new ArrayList<>();
            List<Integer> topList = new ArrayList<>();
            List<Integer> bottomList = new ArrayList<>();
            for (int index : torso)
            {
                if (x[index] < anchor.x) leftList.add(index);
                else rightList.add(index);
                if (y[index] < anchor.y) topList.add(index);
                else bottomList.add(index);
            }
            if (leftList.size() < 3 || rightList.size() < 3
                || topList.size() < 3 || bottomList.size() < 3)
            {
                return null;
            }
            int[] left = toArray(leftList);
            int[] right = toArray(rightList);
            int[] top = toArray(topList);
            int[] bottom = toArray(bottomList);
            Frame frame = frame(model, left, right, top, bottom);
            if (frame == null)
            {
                return null;
            }
            return new TorsoRig(count, torso, left, right, top, bottom, anchor, frame);
        }

        private void apply(Model playerModel, Model weaponModel,
            float[] baseX, float[] baseY, float[] baseZ)
        {
            // Animation can skew the left/right torso vertices enough to spin
            // the gear away from the back when the player changes direction.
            // Follow the torso translation while preserving the fitted angle;
            // the RuneLite object already inherits the player's orientation.
            Vec3 currentAnchor = average(playerModel, torso);
            Vec3 translation = currentAnchor.subtract(referenceAnchor);
            float[] x = weaponModel.getVerticesX();
            float[] y = weaponModel.getVerticesY();
            float[] z = weaponModel.getVerticesZ();
            int count = Math.min(weaponModel.getVerticesCount(), baseX.length);
            for (int i = 0; i < count; i++)
            {
                x[i] = (float) (baseX[i] + translation.x);
                y[i] = (float) (baseY[i] + translation.y);
                z[i] = (float) (baseZ[i] + translation.z);
            }
        }

        private static Frame frame(Model model, int[] left, int[] right,
            int[] top, int[] bottom)
        {
            Vec3 rightAxis = average(model, right).subtract(average(model, left)).normalise();
            Vec3 rawUp = average(model, top).subtract(average(model, bottom));
            Vec3 upAxis = rawUp.subtract(rightAxis.scale(rawUp.dot(rightAxis))).normalise();
            if (!rightAxis.valid() || !upAxis.valid())
            {
                return null;
            }
            Vec3 forwardAxis = upAxis.cross(rightAxis).normalise();
            if (!forwardAxis.valid())
            {
                return null;
            }
            // Re-orthogonalise so animation deformation cannot stretch the axe.
            upAxis = rightAxis.cross(forwardAxis).normalise();
            return new Frame(rightAxis, upAxis, forwardAxis);
        }

        private static Vec3 average(Model model, int[] indices)
        {
            float[] x = model.getVerticesX();
            float[] y = model.getVerticesY();
            float[] z = model.getVerticesZ();
            double sx = 0, sy = 0, sz = 0;
            for (int index : indices)
            {
                sx += x[index]; sy += y[index]; sz += z[index];
            }
            return new Vec3(sx / indices.length, sy / indices.length, sz / indices.length);
        }

        private static int[] toArray(List<Integer> values)
        {
            int[] result = new int[values.size()];
            for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
            return result;
        }
    }

    private static final class Frame
    {
        private final Vec3 right;
        private final Vec3 up;
        private final Vec3 forward;

        private Frame(Vec3 right, Vec3 up, Vec3 forward)
        {
            this.right = right;
            this.up = up;
            this.forward = forward;
        }
    }

    private static final class Vec3
    {
        private final double x;
        private final double y;
        private final double z;

        private Vec3(double x, double y, double z)
        {
            this.x = x; this.y = y; this.z = z;
        }

        private Vec3 add(Vec3 other) { return new Vec3(x + other.x, y + other.y, z + other.z); }
        private Vec3 subtract(Vec3 other) { return new Vec3(x - other.x, y - other.y, z - other.z); }
        private Vec3 scale(double amount) { return new Vec3(x * amount, y * amount, z * amount); }
        private double dot(Vec3 other) { return x * other.x + y * other.y + z * other.z; }
        private Vec3 cross(Vec3 other)
        {
            return new Vec3(y * other.z - z * other.y,
                z * other.x - x * other.z, x * other.y - y * other.x);
        }
        private Vec3 normalise()
        {
            double length = Math.sqrt(dot(this));
            return length < .0001 ? new Vec3(Double.NaN, Double.NaN, Double.NaN)
                : scale(1.0 / length);
        }
        private boolean valid()
        {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        }
    }

    private void destroyObject()
    {
        if (holsteredWeapon != null)
        {
            holsteredWeapon.setActive(false);
            holsteredWeapon = null;
        }
        holsteredModel = null;
        baseWeaponX = null;
        baseWeaponY = null;
        baseWeaponZ = null;
        if (holsteredShield == null)
        {
            torsoRig = null;
            torsoRigCapturedWhileIdle = false;
        }
        builtWeaponItemId = -1;
    }

    private void destroyShield()
    {
        if (holsteredShield != null)
        {
            holsteredShield.setActive(false);
            holsteredShield = null;
        }
        holsteredShieldModel = null;
        baseShieldX = null;
        baseShieldY = null;
        baseShieldZ = null;
        if (holsteredWeapon == null)
        {
            torsoRig = null;
            torsoRigCapturedWhileIdle = false;
        }
        builtShieldItemId = -1;
    }
}
