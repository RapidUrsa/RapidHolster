package com.rapidursa.holster;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.ColorScheme;

@Singleton
final class CustomWeaponPanel extends PluginPanel
{
    private final Client client;
    private final ClientThread thread;
    private final ItemManager items;
    private final ConfigManager manager;
    private final RapidHolsterConfig config;
    private volatile RapidHolsterPlugin plugin;
    private volatile int generation;
    private volatile int searchSequence;
    private final JTextField query = new JTextField();
    private final JCheckBox hideCape = new JCheckBox("Hide cape");
    private final DefaultListModel<Choice> matches = new DefaultListModel<>();
    private final JList<Choice> results = new JList<>(matches);
    private final JLabel status = new JLabel("Search for a weapon or off-hand item.");
    private final JPanel cards = new JPanel();
    // Only accessed on the Swing event thread; writes use immutable snapshots.
    private final Map<Integer, int[]> saved = new LinkedHashMap<>();

    @Inject
    CustomWeaponPanel(Client client, ClientThread thread, ItemManager items,
        ConfigManager manager, RapidHolsterConfig config)
    {
        this.client = client; this.thread = thread; this.items = items;
        this.manager = manager; this.config = config;
        setLayout(new BorderLayout(0, 8));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(hideCape);
        hideCape.setToolTipText("Hide your cape visually, including while your weapon is drawn");
        hideCape.addActionListener(e -> {
            boolean selected = hideCape.isSelected();
            int token = generation;
            thread.invokeLater(() -> {
                if (plugin != null && generation == token)
                    manager.setConfiguration(RapidHolsterPlugin.CONFIG_GROUP, "hideCape", selected);
            });
        });
        top.add(new JLabel("Custom gear")); top.add(query);
        query.setToolTipText("Weapon or off-hand item name or ID; press Enter to search");
        query.addActionListener(e -> search());
        JButton search = new JButton("Search");
        search.addActionListener(e -> search()); top.add(search);
        results.setVisibleRowCount(5);
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        results.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override public Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean selected, boolean focus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Choice) label.setIcon(((Choice) value).image);
                label.setBackground(selected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
                label.setForeground(selected ? Color.WHITE : ColorScheme.TEXT_COLOR);
                label.setOpaque(true);
                return label;
            }
        });
        JScrollPane searchResults = new JScrollPane(results);
        searchResults.setPreferredSize(new Dimension(210, 130));
        top.add(searchResults);
        JButton apply = new JButton("Apply / Add item");
        apply.addActionListener(e -> addSelected()); top.add(apply);
        results.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "apply");
        results.getActionMap().put("apply", new AbstractAction()
        {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { addSelected(); }
        });
        top.add(status); add(top, BorderLayout.NORTH);
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        add(cards, BorderLayout.CENTER);
        theme(this);
        // A GridLayout made every row as tall as the results list. Fix each
        // search control to its own height and allow the plugin panel to scroll.
        for (Component component : top.getComponents())
        {
            JComponent row = (JComponent) component;
            int height = row == searchResults ? 130 : row == status ? 38 : 30;
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setPreferredSize(new Dimension(210, height));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            row.setMinimumSize(new Dimension(0, height));
        }
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    }

    private static void theme(Component component)
    {
        component.setBackground(ColorScheme.DARK_GRAY_COLOR);
        component.setForeground(ColorScheme.TEXT_COLOR);
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) theme(child);
        if (component instanceof javax.swing.text.JTextComponent)
        {
            javax.swing.text.JTextComponent field = (javax.swing.text.JTextComponent) component;
            field.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            field.setCaretColor(ColorScheme.TEXT_COLOR);
            field.setSelectionColor(ColorScheme.MEDIUM_GRAY_COLOR);
            field.setSelectedTextColor(Color.WHITE);
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        }
        if (component instanceof AbstractButton)
        {
            AbstractButton button = (AbstractButton) component;
            button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            button.setOpaque(true);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 6, 5, 6)));
        }
        if (component instanceof JList)
        {
            JList<?> list = (JList<?>) component;
            list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            list.setSelectionBackground(ColorScheme.MEDIUM_GRAY_COLOR);
            list.setSelectionForeground(Color.WHITE);
        }
        if (component instanceof JScrollPane)
        {
            JScrollPane scroll = (JScrollPane) component;
            scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
            scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR));
        }
    }

    static BufferedImage icon()
    {
        try (java.io.InputStream stream = CustomWeaponPanel.class.getResourceAsStream("holster.png"))
        {
            if (stream == null) throw new IllegalStateException("Missing holster icon");
            BufferedImage image = javax.imageio.ImageIO.read(stream);
            if (image == null) throw new IllegalStateException("Invalid holster icon");
            return image;
        }
        catch (java.io.IOException e) { throw new IllegalStateException("Unable to read holster icon", e); }
    }

    void bind(RapidHolsterPlugin plugin)
    {
        this.plugin = plugin;
        int token = ++generation;
        String encoded = config.itemPlacements();
        SwingUtilities.invokeLater(() -> {
            if (generation != token) return;
            hideCape.setSelected(config.hideCape());
            saved.clear(); cards.removeAll(); matches.clear();
            for (String entry : encoded.split(";"))
            {
                try
                {
                    String[] pair = entry.split(":");
                    if (pair.length != 2) continue;
                    int id = Integer.parseInt(pair[0].trim());
                    String[] fields = pair[1].split(",");
                    if (id < 0 || fields.length != 7) continue;
                    int[] v = new int[7]; boolean valid = true;
                    for (int i = 0; i < 7; i++)
                    {
                        v[i] = Integer.parseInt(fields[i].trim());
                        if (v[i] < min(i) || v[i] > max(i)) valid = false;
                    }
                    if (valid) saved.put(id, v);
                }
                catch (NumberFormatException ignored) { }
            }
            for (Integer id : saved.keySet())
            {
                thread.invokeLater(() -> {
                    if (plugin != this.plugin || generation != token) return;
                    Choice choice = choice(id);
                    SwingUtilities.invokeLater(() -> {
                        if (generation == token && saved.containsKey(id)) addCard(choice, false);
                    });
                });
            }
            revalidate(); repaint();
        });
    }
    void unbind() { plugin = null; generation++; searchSequence++; }

    void syncHideCape()
    {
        int token = generation;
        boolean selected = config.hideCape();
        SwingUtilities.invokeLater(() -> {
            if (plugin != null && generation == token) hideCape.setSelected(selected);
        });
    }

    private Choice choice(int id)
    { return new Choice(id, items.getItemComposition(id).getName(), new ImageIcon(items.getImage(id))); }
    private static String normal(String value)
    { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    private static boolean matchesName(String name, String term)
    {
        if (normal(name).contains(normal(term))) return true;
        if (name == null) return false;
        for (String word : term.toLowerCase(Locale.ROOT).split("\\s+"))
            if (!name.toLowerCase(Locale.ROOT).contains(word)) return false;
        return true;
    }
    private void search()
    {
        String term = query.getText().trim();
        int searchId = ++searchSequence;
        if (normal(term).length() < 2) { status.setText("Enter at least two letters or an ID."); return; }
        int token = generation;
        matches.clear(); status.setText("Searching…");
        int[] cursor = {0}; List<Choice> found = new ArrayList<>();
        thread.invokeLater((java.util.function.BooleanSupplier) () -> {
            if (plugin == null || generation != token || searchSequence != searchId) return true;
            if (client.getGameState() != GameState.LOGGED_IN)
            {
                SwingUtilities.invokeLater(() -> {
                    if (generation == token && searchSequence == searchId) status.setText("Log in to search weapons.");
                });
                return true;
            }
            int end = Math.min(cursor[0] + 250, client.getItemCount());
            for (int id = cursor[0]; id < end; id++)
            {
                ItemComposition item = items.getItemComposition(id);
                if (!(Integer.toString(id).equals(term) || matchesName(item.getName(), term))
                    || item.getNote() != -1 || item.getPlaceholderTemplateId() != -1) continue;
                ItemStats stats = items.getItemStats(id);
                String[] actions = item.getInventoryActions();
                boolean gear = stats != null && stats.getEquipment() != null
                    ? stats.getEquipment().getSlot() == 3 || stats.getEquipment().getSlot() == 5
                    : actions != null && Arrays.asList(actions).contains("Wield");
                if (gear) found.add(choice(id));
            }
            cursor[0] = end;
            if (end < client.getItemCount()) return false;
            found.sort(Comparator.comparing((Choice c) -> !(normal(c.name).equals(normal(term)) || Integer.toString(c.id).equals(term)))
                .thenComparing(c -> c.name).thenComparingInt(c -> c.id));
            SwingUtilities.invokeLater(() -> {
                if (generation != token || searchSequence != searchId || !query.getText().trim().equals(term)) return;
                matches.clear(); found.stream().limit(100).forEach(matches::addElement);
                status.setText(found.isEmpty() ? "No weapons or off-hand items found." : "Select a result and click Apply.");
                if (!found.isEmpty()) { results.setSelectedIndex(0); results.requestFocusInWindow(); }
            });
            return true;
        });
    }
    private void addSelected()
    {
        Choice choice = results.getSelectedValue();
        if (choice == null || plugin == null) return;
        if (saved.containsKey(choice.id)) { status.setText("Already saved below."); return; }
        int token = generation;
        thread.invokeLater(() -> {
            RapidHolsterPlugin current = plugin;
            if (current == null || generation != token) return;
            int[] defaults = current.defaultPlacement(choice.id);
            SwingUtilities.invokeLater(() -> {
                if (generation != token || saved.containsKey(choice.id)) return;
                saved.put(choice.id, defaults); addCard(choice, true); save();
                status.setText("Added. Equip and holster to adjust.");
            });
        });
    }
    private void addCard(Choice choice, boolean expanded)
    {
        JPanel card = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new GridLayout(0, 2, 4, 4));
        JToggleButton header = new JToggleButton((expanded ? "▼ " : "▶ ") + choice.name, choice.image, expanded);
        controls.setVisible(expanded);
        header.setToolTipText("Item " + choice.id + " — click to expand/collapse");
        header.addActionListener(e -> {
            controls.setVisible(header.isSelected());
            header.setText((header.isSelected() ? "▼ " : "▶ ") + choice.name);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
            revalidate(); repaint();
        });
        card.add(header, BorderLayout.NORTH); card.add(controls, BorderLayout.CENTER);
        String[] labels = {"Sideways", "Height", "Forward", "Pitch", "Yaw", "Roll", "Scale"};
        JSpinner[] spinners = new JSpinner[7]; boolean[] updating = {false};
        for (int i = 0; i < 7; i++)
        {
            final int index = i;
            controls.add(new JLabel(labels[i]));
            spinners[i] = new JSpinner(new SpinnerNumberModel(saved.get(choice.id)[i], min(i), max(i), 1));
            spinners[i].addChangeListener(e -> {
                if (updating[0] || !saved.containsKey(choice.id)) return;
                saved.get(choice.id)[index] = ((Number) spinners[index].getValue()).intValue(); save();
            });
            controls.add(spinners[i]);
        }
        JButton reset = new JButton("Reset"); reset.setToolTipText("Copy category defaults for this weapon");
        reset.addActionListener(e -> {
            int token = generation;
            thread.invokeLater(() -> {
                RapidHolsterPlugin current = plugin;
                if (current == null || generation != token) return;
                int[] defaults = current.defaultPlacement(choice.id);
                SwingUtilities.invokeLater(() -> {
                    if (generation != token || !saved.containsKey(choice.id)) return;
                    saved.put(choice.id, defaults); updating[0] = true;
                    for (int i = 0; i < 7; i++) spinners[i].setValue(defaults[i]);
                    updating[0] = false; save();
                });
            });
        });
        JButton remove = new JButton("Remove");
        remove.addActionListener(e -> {
            saved.remove(choice.id); cards.remove(card); save(); revalidate(); repaint();
        });
        controls.add(reset); controls.add(remove);
        card.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        theme(card);
        header.setForeground(ColorScheme.BRAND_ORANGE);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        cards.add(card); revalidate(); repaint();
    }
    private static int min(int i) { return i == 6 ? 25 : i < 3 ? -200 : -180; }
    private static int max(int i) { return i == 6 || i < 3 ? 200 : 180; }
    private void save()
    {
        StringBuilder text = new StringBuilder();
        saved.forEach((id, v) -> {
            if (text.length() > 0) text.append(';');
            text.append(id).append(':');
            for (int i = 0; i < 7; i++) { if (i > 0) text.append(','); text.append(v[i]); }
        });
        String snapshot = text.toString(); int token = generation;
        thread.invokeLater(() -> {
            if (plugin != null && generation == token)
                manager.setConfiguration(RapidHolsterPlugin.CONFIG_GROUP, "itemPlacements", snapshot);
        });
    }
    private static final class Choice
    {
        final int id; final String name; final ImageIcon image;
        Choice(int id, String name, ImageIcon image) { this.id = id; this.name = name; this.image = image; }
        @Override public String toString() { return name + " (" + id + ")"; }
    }
}
