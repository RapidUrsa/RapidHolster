package com.rapidursa.holster;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
final class HolsterToggleOverlay extends Overlay implements MouseListener
{
    private final ClientThread clientThread;
    private final RapidHolsterConfig config;
    private final ItemManager itemManager;
    private RapidHolsterPlugin plugin;
    private int cachedItemId = -1;
    private BufferedImage cachedIcon;

    @Inject
    HolsterToggleOverlay(ClientThread clientThread, RapidHolsterConfig config,
        ItemManager itemManager)
    {
        this.clientThread = clientThread;
        this.config = config;
        this.itemManager = itemManager;
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    void bind(RapidHolsterPlugin plugin)
    {
        this.plugin = plugin;
    }

    void unbind()
    {
        plugin = null;
        cachedItemId = -1;
        cachedIcon = null;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        RapidHolsterPlugin current = plugin;
        int itemId = current == null ? -1 : current.getEquippedWeaponItemId();
        if (!config.showButton() || current == null || !current.isSupportedWeapon(itemId))
        {
            return null;
        }
        if (itemId != cachedItemId)
        {
            cachedItemId = itemId;
            cachedIcon = itemManager.getImage(itemId);
        }

        int size = Math.max(24, Math.min(96, config.buttonSize()));
        int padding = Math.max(2, size / 12);
        boolean holstered = current.isHolstered();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(18, 20, 24, 210));
        graphics.fillRoundRect(0, 0, size, size,
            Math.max(7, size / 5), Math.max(7, size / 5));
        graphics.setColor(holstered ? new Color(231, 178, 83)
            : new Color(115, 120, 128));
        graphics.setStroke(new BasicStroke(Math.max(1f, size / 22f)));
        graphics.drawRoundRect(1, 1, size - 3, size - 3,
            Math.max(7, size / 5), Math.max(7, size / 5));

        if (cachedIcon != null)
        {
            Composite previous = graphics.getComposite();
            if (!holstered)
            {
                graphics.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, .48f));
            }
            graphics.drawImage(cachedIcon, padding, padding,
                size - 2 * padding, size - 2 * padding, null);
            graphics.setComposite(previous);
        }
        return new Dimension(size, size);
    }

    @Override
    public MouseEvent mousePressed(MouseEvent event)
    {
        if (config.showButton() && plugin != null
            && plugin.isSupportedWeapon(plugin.getEquippedWeaponItemId())
            && !event.isAltDown() && SwingUtilities.isLeftMouseButton(event)
            && getBounds().contains(event.getPoint()))
        {
            event.consume();
            RapidHolsterPlugin current = plugin;
            if (current != null)
            {
                clientThread.invokeLater(current::toggleHolstered);
            }
        }
        return event;
    }

    @Override public MouseEvent mouseClicked(MouseEvent event) { return event; }
    @Override public MouseEvent mouseReleased(MouseEvent event) { return event; }
    @Override public MouseEvent mouseEntered(MouseEvent event) { return event; }
    @Override public MouseEvent mouseExited(MouseEvent event) { return event; }
    @Override public MouseEvent mouseDragged(MouseEvent event) { return event; }
    @Override public MouseEvent mouseMoved(MouseEvent event) { return event; }
}
