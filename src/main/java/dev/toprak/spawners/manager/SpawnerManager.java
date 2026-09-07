package dev.toprak.spawners.manager;

import dev.toprak.spawners.SpawnersPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SpawnerManager {
    private final SpawnersPlugin plugin;
    private final NamespacedKey entityKey;
    private final NamespacedKey countKey;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SpawnerManager(SpawnersPlugin plugin) {
        this.plugin = plugin;
        this.entityKey = new NamespacedKey(plugin, "spawner_entity");
        this.countKey = new NamespacedKey(plugin, "spawner_count");
    }

    public ItemStack createSpawnerItem(EntityType type, int amount) {
        ItemStack item = new ItemStack(Material.SPAWNER, Math.max(1, amount));
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta != null) {
            String typeName = formatName(type.name());
            meta.displayName(mm.deserialize("<yellow><bold>" + typeName + " Spawner</bold></yellow>"));
            meta.lore(List.of(
                mm.deserialize("<gray>Type: <gold>" + typeName + "</gold></gray>"),
                mm.deserialize("<gray>Place on another spawner to stack.</gray>")
            ));

            CreatureSpawner cs = (CreatureSpawner) meta.getBlockState();
            cs.setSpawnedType(type);
            meta.setBlockState(cs);

            meta.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, type.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    public EntityType getEntityTypeFromItem(ItemStack item) {
        if (item == null || item.getType() != Material.SPAWNER) return null;
        if (item.getItemMeta() instanceof BlockStateMeta bsm) {
            String pEntity = bsm.getPersistentDataContainer().get(entityKey, PersistentDataType.STRING);
            if (pEntity != null) {
                try {
                    return EntityType.valueOf(pEntity.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            if (bsm.getBlockState() instanceof CreatureSpawner cs) {
                return cs.getSpawnedType();
            }
        }
        return null;
    }

    public int getStackCount(Block block) {
        if (!(block.getState() instanceof CreatureSpawner cs)) return 1;
        PersistentDataContainer pdc = cs.getPersistentDataContainer();
        Integer count = pdc.get(countKey, PersistentDataType.INTEGER);
        return count != null ? Math.max(1, count) : 1;
    }

    public void setStackCount(Block block, int count) {
        if (!(block.getState() instanceof CreatureSpawner cs)) return;
        cs.getPersistentDataContainer().set(countKey, PersistentDataType.INTEGER, Math.max(1, count));
        cs.update(true, false);
    }

    public String formatName(String raw) {
        if (raw == null) return "Unknown";
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1).toLowerCase())
              .append(" ");
        }
        return sb.toString().trim();
    }
}
