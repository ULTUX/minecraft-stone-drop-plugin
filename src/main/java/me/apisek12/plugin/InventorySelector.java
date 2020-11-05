package me.apisek12.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InventorySelector implements Listener {
    private Player player;
    private HashMap<String, Setting> settings;
    private static String title = ChatColor.DARK_AQUA + "Item Drop Chances";
    private Inventory selector;
    private LinkedHashMap<ItemStack, ArrayList<ItemStack>> items = new LinkedHashMap<>();
    private static HashMap<Player, InventorySelector> objects = new HashMap<>();
    private boolean willBeUsed = false;
    private static Inventory secondaryWindow;
    private static ItemStack exit, back;

    static {
        exit = new ItemStack(Material.BARRIER);
        back = new ItemStack(Material.ARROW);
        ItemMeta exitMeta = exit.getItemMeta();
        ItemMeta backMeta = back.getItemMeta();

        exitMeta.setDisplayName(ChatColor.RED+"Exit");
        backMeta.setDisplayName(ChatColor.GREEN+"Back");
        exitMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        backMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        exit.setItemMeta(exitMeta);
        back.setItemMeta(backMeta);

    }

    public InventorySelector() {}

    public InventorySelector(Player player, HashMap<String, Setting> settings) {
        this.player = player;
        this.settings = settings;
        objects.put(player, this);
        selector = Bukkit.createInventory(null, PluginMain.dropChances.size() + (9 - PluginMain.dropChances.size() % 9) + 2 * 9, title);
        reloadInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 255, 0);
        player.openInventory(selector);
    }

    private void refreshSettings(){
        items.clear();
        settings.forEach((materialName, setting) -> {
            Material material;
            if ((material = Material.getMaterial(materialName)) != null) {
                DropChance dropData = PluginMain.dropChances.get(materialName);
                if (dropData != null) {
                    ItemStack item = new ItemStack(material);
                    ItemMeta itemMeta = item.getItemMeta();
                    if (dropData != null && dropData.getEnchant() != null)
                        dropData.getEnchant().forEach((enchantment, integer) -> itemMeta.addEnchant(enchantment, integer, false));
                    ArrayList<String> lore = new ArrayList<>();
                    String onOff;
                    if (setting.isOn()) onOff = ChatColor.GREEN + "enabled";
                    else onOff = ChatColor.RED + "disabled";
                    lore.add("");
                    lore.add(ChatColor.DARK_GRAY + "--------------------");
                    lore.add(ChatColor.GRAY + "This item drop is " + onOff + ".");
                    lore.add(ChatColor.AQUA + "Right click to toggle.");
                    lore.add(ChatColor.AQUA + "Left click to see details.");
                    lore.add(ChatColor.DARK_GRAY + "--------------------");

                    itemMeta.setLore(lore);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    item.setItemMeta(itemMeta);
                    ArrayList<ItemStack> dropChances = getItemDetailedData(dropData, material);

                    items.put(item, dropChances);
                }
            }
        });
    }

    private ArrayList<ItemStack> getItemDetailedData(DropChance dropData, Material dropMaterial) {
        ArrayList<ItemStack> items = new ArrayList<>();
        ItemStack f0, f1, f2, f3;

        f0 = new ItemStack(dropMaterial);
        f1 = new ItemStack(dropMaterial);
        f2 = new ItemStack(dropMaterial);
        f3 = new ItemStack(dropMaterial);

        items.add(f0);
        items.add(f1);
        items.add(f2);
        items.add(f3);

        if (dropData != null && dropData.getEnchant() != null) {
            f0.addEnchantments(dropData.getEnchant());
            f1.addEnchantments(dropData.getEnchant());
            f2.addEnchantments(dropData.getEnchant());
            f3.addEnchantments(dropData.getEnchant());
        }

        ItemMeta f0Meta = f0.getItemMeta();
        ItemMeta f1Meta = f2.getItemMeta();
        ItemMeta f2Meta = f2.getItemMeta();
        ItemMeta f3Meta = f3.getItemMeta();

        f0Meta.setDisplayName(ChatColor.GREEN + "No fortune");
        f1Meta.setDisplayName(ChatColor.GREEN + "Fortune 1");
        f2Meta.setDisplayName(ChatColor.GREEN + "Fortune 2");
        f3Meta.setDisplayName(ChatColor.GREEN + "Fortune 3");

        f0Meta.setLore(generateItemLore(dropData, 0));
        f1Meta.setLore(generateItemLore(dropData, 1));
        f2Meta.setLore(generateItemLore(dropData, 2));
        f3Meta.setLore(generateItemLore(dropData, 3));

        f0Meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        f1Meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        f2Meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        f3Meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        f0.setItemMeta(f0Meta);
        f1.setItemMeta(f1Meta);
        f2.setItemMeta(f2Meta);
        f3.setItemMeta(f3Meta);

        return items;

    }

    private ArrayList<String> generateItemLore(DropChance dropData, int level) {
        ArrayList<String> lore = new ArrayList<>();
        double chance = 0;
        int min = 0, max = 0;
        assert level == 0 || level == 1 || level == 2 || level == 3;
        DecimalFormat format = new DecimalFormat("##0.0##");
        switch (level) {
            case 0:
                chance = dropData.getNof();
                min = dropData.getMinnof();
                max = dropData.getMaxnof();
                break;
            case 1:
                chance = dropData.getF1();
                min = dropData.getMinf1();
                max = dropData.getMaxnof();
                break;
            case 2:
                chance = dropData.getF2();
                min = dropData.getMinf2();
                max = dropData.getMaxf2();
                break;
            case 3:
                chance = dropData.getF3();
                min = dropData.getMinf3();
                max = dropData.getMaxf3();
                break;
        }
        chance *= 100;
        lore.add(ChatColor.GRAY + "Drop chance: " + ChatColor.GOLD + format.format(chance)+"%");
        lore.add(ChatColor.GRAY + "Drop amount: " + ChatColor.GOLD + min + "-" + max);

        return lore;
    }

    private void reloadInventory() {
        refreshSettings();
        selector.clear();
        AtomicInteger index = new AtomicInteger(selector.getSize() - 10 - PluginMain.dropChances.size());
        items.forEach((itemStack, itemStacks) -> selector.setItem(index.getAndIncrement(), itemStack));
        selector.setItem(selector.getSize()-5, exit);
    }
    private void openSecondaryWindow(ArrayList<ItemStack> items){
        willBeUsed = false;
        player.playSound(player.getLocation(), Sound.UI_LOOM_TAKE_RESULT, 255, 1);
        secondaryWindow = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA+"Drop information");
        AtomicInteger i = new AtomicInteger(10);
        items.forEach(item -> secondaryWindow.setItem(i.getAndAdd(2), item));
        secondaryWindow.setItem(secondaryWindow.getSize()-5, exit);
        secondaryWindow.setItem(secondaryWindow.getSize()-6, back);
        player.openInventory(secondaryWindow);
    }
    @EventHandler
    public void InventoryClickEvent(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getCurrentItem() == null) return;
        if (objects.containsKey(event.getWhoClicked()) && event.getClickedInventory().equals(objects.get(event.getWhoClicked()).selector)) {
            event.setCancelled(true);
            if (checkForFuncButtonsPressed(event)) return;
            InventorySelector inventorySelector = objects.get(event.getWhoClicked());
            Player player = inventorySelector.player;
            ItemStack clickedItem = event.getCurrentItem();
            if (event.isRightClick()) {
                inventorySelector.settings.get(clickedItem.getType().toString()).toggle();
                player.playSound(player.getLocation(), Sound.UI_STONECUTTER_SELECT_RECIPE, 255, 1);
                inventorySelector.reloadInventory();
            } else if (event.isLeftClick()) {
               if (event.getCurrentItem() != null){
                   inventorySelector.willBeUsed = true;
                   player.closeInventory();
                   inventorySelector.openSecondaryWindow(inventorySelector.items.get(clickedItem));
               }
            }
        }
        if (objects.containsKey(event.getWhoClicked()) && event.getClickedInventory().equals(objects.get(event.getWhoClicked()).secondaryWindow)) {
            event.setCancelled(true);
            if (checkForFuncButtonsPressed(event)) return;
            ((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.ENTITY_CHICKEN_EGG, 255, 1);
            checkForFuncButtonsPressed(event);
        }
    }

    @EventHandler
    public void InventoryCloseEvent(InventoryCloseEvent event) {
        if (objects.containsKey(event.getPlayer())) {
            if (event.getInventory().equals(objects.get(event.getPlayer()).selector)){
                ((Player) event.getPlayer()).playSound(event.getPlayer().getLocation(), Sound.UI_LOOM_TAKE_RESULT, 255, 1);
            }
            else if (event.getInventory().equals(objects.get(event.getPlayer()).secondaryWindow)){
                ((Player) event.getPlayer()).playSound(event.getPlayer().getLocation(), Sound.UI_LOOM_TAKE_RESULT, 255, 1);
            }
            if (!objects.get(event.getPlayer()).willBeUsed) objects.remove(event.getPlayer());
        }
    }

    private boolean checkForFuncButtonsPressed(InventoryClickEvent event){
        if (event.getCurrentItem() != null){
            if (event.getCurrentItem().equals(exit)) {
                event.getWhoClicked().closeInventory();
                return true;
            }
            else if (event.getCurrentItem().equals(back)) {
                objects.get(event.getWhoClicked()).willBeUsed = true;
                event.getWhoClicked().closeInventory();
                objects.get(event.getWhoClicked()).player.openInventory(objects.get(event.getWhoClicked()).selector);
                objects.get(event.getWhoClicked()).reloadInventory();
                return true;
            }

        }
        return false;
    }

}