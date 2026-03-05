package com.nettyforge.cordsync.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.nettyforge.cordsync.CordSync;
import com.nettyforge.cordsync.storage.StorageProvider;
import com.nettyforge.cordsync.utils.MessageUtil;

/**
 * Premium in-game GUI menu for CordSync.
 * Fully localized through locales config files.
 */
public class LinkGUI implements Listener {

        private final CordSync plugin;

        public LinkGUI(CordSync plugin) {
                this.plugin = plugin;
        }

        private String getTitle() {
                return MessageUtil.get("gui.title");
        }

        public void open(Player player) {
                Inventory gui = Bukkit.createInventory(null, 27, getTitle());
                UUID uuid = player.getUniqueId();
                StorageProvider storage = plugin.getStorageProvider();
                boolean isLinked = storage.isPlayerLinked(uuid);

                ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new ArrayList<>());
                for (int i = 0; i < 27; i++) {
                        gui.setItem(i, glass);
                }

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                if (skullMeta != null) {
                        skullMeta.setOwningPlayer(player);

                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("player", player.getName());
                        skullMeta.setDisplayName(MessageUtil.format("gui.head-name", placeholders));

                        List<String> lore = new ArrayList<>();
                        lore.add("");
                        if (isLinked) {
                                String discordId = storage.getDiscordId(uuid);
                                lore.add(MessageUtil.get("gui.status-linked"));

                                Map<String, String> idMap = new HashMap<>();
                                idMap.put("id", discordId != null ? discordId : "Unknown");
                                lore.add(MessageUtil.format("gui.discord-id", idMap));
                        } else {
                                lore.add(MessageUtil.get("gui.status-not-linked"));
                        }
                        lore.add("");
                        lore.add(MessageUtil.get("gui.version-footer").replace("{version}",
                                        plugin.getDescription().getVersion()));

                        skullMeta.setLore(lore);
                        head.setItemMeta(skullMeta);
                }
                gui.setItem(4, head);

                if (isLinked) {
                        gui.setItem(11, createItem(Material.EMERALD_BLOCK,
                                        MessageUtil.get("gui.btn-status-name"),
                                        MessageUtil.getList("gui.btn-status-lore")));

                        gui.setItem(13, createItem(Material.BARRIER,
                                        MessageUtil.get("gui.btn-unlink-name"),
                                        MessageUtil.getList("gui.btn-unlink-lore")));

                        gui.setItem(15, createItem(Material.CHEST,
                                        MessageUtil.get("gui.btn-rewards-name"),
                                        MessageUtil.getList("gui.btn-rewards-lore")));
                } else {
                        gui.setItem(11, createItem(Material.LIME_DYE,
                                        MessageUtil.get("gui.btn-link-name"),
                                        MessageUtil.getList("gui.btn-link-lore")));

                        gui.setItem(13, createItem(Material.BOOK,
                                        MessageUtil.get("gui.btn-info-name"),
                                        MessageUtil.getList("gui.btn-info-lore")));

                        gui.setItem(15, createItem(Material.GOLD_INGOT,
                                        MessageUtil.get("gui.btn-preview-name"),
                                        MessageUtil.getList("gui.btn-preview-lore")));
                }

                player.openInventory(gui);
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
                if (event.getView().getTitle() == null)
                        return;
                if (!event.getView().getTitle().equals(getTitle()))
                        return;

                event.setCancelled(true);

                if (!(event.getWhoClicked() instanceof Player))
                        return;
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                boolean isLinked = plugin.getStorageProvider().isPlayerLinked(player.getUniqueId());

                if (isLinked) {
                        switch (slot) {
                                case 11:
                                        player.closeInventory();
                                        List<String> msgs = MessageUtil.getList("gui.status-chat-msg");
                                        String discordId = plugin.getStorageProvider()
                                                        .getDiscordId(player.getUniqueId());
                                        for (String msg : msgs) {
                                                player.sendMessage(msg.replace("{id}",
                                                                discordId != null ? discordId : "Unknown"));
                                        }
                                        break;
                                case 13:
                                        player.closeInventory();
                                        player.performCommand("unlink");
                                        break;
                                case 15:
                                        player.closeInventory();
                                        player.sendMessage(MessageUtil.get("gui.rewards-chat-msg"));
                                        break;
                        }
                } else {
                        switch (slot) {
                                case 11:
                                        player.closeInventory();
                                        player.performCommand("link");
                                        break;
                                case 13:
                                        break; // Just view info
                        }
                }
        }

        private ItemStack createItem(Material material, String name, List<String> lore) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                        meta.setDisplayName(name);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                }
                return item;
        }
}
