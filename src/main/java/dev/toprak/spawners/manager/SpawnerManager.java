package dev.toprak.spawners.manager;

import dev.toprak.spawners.SpawnersPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpawnerManager {
    /** Vanilla max stack size of a spawner item. Larger amounts are split into several stacks. */
    public static final int ITEM_MAX_STACK = 64;
    /** Hard cap on how many spawners a single give/drop may produce (36 full stacks). */
    public static final int MAX_GIVE_AMOUNT = ITEM_MAX_STACK * 36;

    private final SpawnersPlugin plugin;
    private final NamespacedKey entityKey;
    private final NamespacedKey countKey;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SpawnerManager(SpawnersPlugin plugin) {
        this.plugin = plugin;
        this.entityKey = new NamespacedKey(plugin, "spawner_entity");
        this.countKey = new NamespacedKey(plugin, "spawner_count");
    }

    /** Only living, spawnable mob types may be written into a spawner; anything else is treated as empty. */
    public static boolean isValidSpawnerType(EntityType type) {
        return type != null && type != EntityType.UNKNOWN && type != EntityType.PLAYER
                && type.isAlive() && type.isSpawnable();
    }

    /** Creates a single spawner item stack. {@code type} may be null for an empty spawner. Amount is clamped to [1, 64]. */
    public ItemStack createSpawnerItem(EntityType type, int amount) {
        if (!isValidSpawnerType(type)) type = null;
        ItemStack item = new ItemStack(Material.SPAWNER, Math.max(1, Math.min(ITEM_MAX_STACK, amount)));
        if (item.getItemMeta() instanceof BlockStateMeta meta) {
            String typeName = typeName(type);
            meta.displayName(mm.deserialize("<yellow><bold>" + typeName + " Spawner</bold></yellow>"));
            meta.lore(List.of(
                mm.deserialize("<gray>Type: <gold>" + typeName + "</gold></gray>"),
                mm.deserialize("<gray>Place on another spawner to stack.</gray>")
            ));

            if (meta.getBlockState() instanceof CreatureSpawner cs) {
                cs.setSpawnedType(type);
                meta.setBlockState(cs);
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (type != null) {
                pdc.set(entityKey, PersistentDataType.STRING, type.name());
            } else {
                pdc.remove(entityKey);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Splits {@code total} spawners into vanilla-sized stacks so nothing is lost to overstacking. */
    public List<ItemStack> createSpawnerItems(EntityType type, int total) {
        int remaining = Math.max(1, Math.min(MAX_GIVE_AMOUNT, total));
        List<ItemStack> items = new ArrayList<>();
        while (remaining > 0) {
            int size = Math.min(ITEM_MAX_STACK, remaining);
            items.add(createSpawnerItem(type, size));
            remaining -= size;
        }
        return items;
    }

    public void dropSpawners(Location location, EntityType type, int total) {
        for (ItemStack stack : createSpawnerItems(type, total)) {
            location.getWorld().dropItemNaturally(location, stack);
        }
    }

    /** Resolves the mob type stored on a spawner item, or null if it is an empty/plain spawner. */
    public EntityType getEntityTypeFromItem(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER) return null;
        if (!(item.getItemMeta() instanceof BlockStateMeta bsm)) return null;

        String pEntity = bsm.getPersistentDataContainer().get(entityKey, PersistentDataType.STRING);
        if (pEntity != null) {
            try {
                EntityType type = EntityType.valueOf(pEntity.toUpperCase(Locale.ROOT));
                if (isValidSpawnerType(type)) return type;
            } catch (IllegalArgumentException ignored) {}
        }
        if (bsm.hasBlockState() && bsm.getBlockState() instanceof CreatureSpawner cs) {
            EntityType type = cs.getSpawnedType();
            if (isValidSpawnerType(type)) return type;
        }
        return null;
    }

    public int getStackCount(Block block) {
        if (!(block.getState() instanceof CreatureSpawner cs)) return 1;
        return getStackCount(cs);
    }

    public int getStackCount(CreatureSpawner cs) {
        Integer count = cs.getPersistentDataContainer().get(countKey, PersistentDataType.INTEGER);
        return count != null ? Math.max(1, count) : 1;
    }

    public void setStackCount(Block block, int count) {
        if (!(block.getState() instanceof CreatureSpawner cs)) return;
        cs.getPersistentDataContainer().set(countKey, PersistentDataType.INTEGER, Math.max(1, count));
        cs.update(true, false);
    }

    public int getMaxStackSize() {
        return Math.max(1, plugin.getConfig().getInt("max-stack-size", 50));
    }

    public static String typeName(EntityType type) {
        return type == null ? "Empty" : formatName(type.name());
    }

    public static String formatName(String raw) {
        if (raw == null) return "Unknown";
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1).toLowerCase(Locale.ROOT))
              .append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Pickaxe tier by material name: WOODEN/GOLDEN=0, STONE=1, IRON=2, DIAMOND=3, NETHERITE=4.
     * Returns -1 for anything that is not a pickaxe.
     */
    public static int pickaxeTier(String materialName) {
        if (materialName == null) return -1;
        return switch (materialName.toUpperCase(Locale.ROOT)) {
            case "WOODEN_PICKAXE", "GOLDEN_PICKAXE" -> 0;
            case "STONE_PICKAXE" -> 1;
            case "IRON_PICKAXE" -> 2;
            case "DIAMOND_PICKAXE" -> 3;
            case "NETHERITE_PICKAXE" -> 4;
            default -> -1;
        };
    }

    /** Parses a config tier name (WOODEN, STONE, IRON, DIAMOND, NETHERITE). Unknown/empty values mean no tier requirement. */
    public static int requiredTier(String configValue) {
        if (configValue == null || configValue.isBlank()) return -1;
        return pickaxeTier(configValue.trim().toUpperCase(Locale.ROOT) + "_PICKAXE");
    }

    public String message(String key, String def) {
        String s = plugin.getConfig().getString("messages." + key);
        return s == null || s.isBlank() ? def : s;
    }
}
