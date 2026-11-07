package com.voltic.lobby.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for creating ItemStacks with modern Paper Adventure components and PDC tags.
 */
public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(@NotNull Material material) {
        this(material, 1);
    }

    public ItemBuilder(@NotNull Material material, int amount) {
        this.itemStack = new ItemStack(material, Math.max(1, amount));
    }

    public ItemBuilder(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    @NotNull
    public static ItemBuilder from(@NotNull Material material) {
        return new ItemBuilder(material);
    }

    @NotNull
    public static ItemBuilder from(@NotNull ItemStack item) {
        return new ItemBuilder(item);
    }

    @NotNull
    public ItemBuilder amount(int amount) {
        itemStack.setAmount(Math.max(1, amount));
        return this;
    }

    @NotNull
    public ItemBuilder name(@Nullable Component name) {
        return editMeta(meta -> meta.displayName(name));
    }

    @NotNull
    public ItemBuilder name(@Nullable String miniMessage) {
        return name(miniMessage != null ? TextUtil.parse(miniMessage) : null);
    }

    @NotNull
    public ItemBuilder lore(@NotNull List<Component> lore) {
        return editMeta(meta -> meta.lore(lore));
    }

    @NotNull
    public ItemBuilder lore(@NotNull Component... lore) {
        return lore(Arrays.asList(lore));
    }

    @NotNull
    public ItemBuilder loreStrings(@NotNull List<String> lore) {
        List<Component> components = new ArrayList<>(lore.size());
        for (String line : lore) {
            components.add(TextUtil.parse(line));
        }
        return lore(components);
    }

    @NotNull
    public ItemBuilder loreStrings(@NotNull String... lore) {
        return loreStrings(Arrays.asList(lore));
    }

    @NotNull
    public ItemBuilder addLore(@NotNull Component line) {
        return editMeta(meta -> {
            List<Component> current = meta.lore();
            if (current == null) {
                current = new ArrayList<>();
            } else {
                current = new ArrayList<>(current);
            }
            current.add(line);
            meta.lore(current);
        });
    }

    @NotNull
    public ItemBuilder addLore(@NotNull String miniMessage) {
        return addLore(TextUtil.parse(miniMessage));
    }

    @NotNull
    public ItemBuilder flags(@NotNull ItemFlag... flags) {
        return editMeta(meta -> meta.addItemFlags(flags));
    }

    @NotNull
    public ItemBuilder hideAllFlags() {
        return flags(ItemFlag.values());
    }

    @NotNull
    public ItemBuilder enchant(@NotNull Enchantment enchantment, int level) {
        return editMeta(meta -> meta.addEnchant(enchantment, level, true));
    }

    @NotNull
    public ItemBuilder glow(boolean glow) {
        return editMeta(meta -> {
            try {
                meta.setEnchantmentGlintOverride(glow);
            } catch (NoSuchMethodError | Exception e) {
                if (glow) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
        });
    }

    @NotNull
    public ItemBuilder customModelData(@Nullable Integer data) {
        return editMeta(meta -> meta.setCustomModelData(data));
    }

    @NotNull
    public ItemBuilder tagString(@NotNull NamespacedKey key, @NotNull String value) {
        return editMeta(meta -> meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value));
    }

    @NotNull
    public ItemBuilder tagInt(@NotNull NamespacedKey key, int value) {
        return editMeta(meta -> meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value));
    }

    @NotNull
    public ItemBuilder editMeta(@NotNull Consumer<ItemMeta> consumer) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            consumer.accept(meta);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    @NotNull
    public ItemStack build() {
        return itemStack.clone();
    }
}
