package dev.toprak.spawners.listener;

import dev.toprak.spawners.SpawnersPlugin;
import dev.toprak.spawners.manager.SpawnerManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerListener implements Listener {
    private final SpawnersPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SpawnerListener(SpawnersPlugin plugin) {
        this.plugin = plugin;
    }

    private SpawnerManager manager() {
        return plugin.getSpawnerManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        ItemStack inHand = event.getItemInHand();
        if (inHand.getType() != Material.SPAWNER) return;

        SpawnerManager manager = manager();
        // null == empty spawner (vanilla 1.20+ allows it); never guess a type.
        EntityType itemType = manager.getEntityTypeFromItem(inHand);

        Block against = event.getBlockAgainst();
        if (against.getType() == Material.SPAWNER && against.getState() instanceof CreatureSpawner target
                && target.getSpawnedType() == itemType) {
            // Stacking: never place a block, always cancel.
            event.setCancelled(true);

            int current = manager.getStackCount(target);
            int max = manager.getMaxStackSize();
            if (current >= max) {
                player.sendMessage(mm.deserialize(manager.message("max-stacked",
                        "<red>This spawner has already reached the maximum stack size (%max%).</red>")
                        .replace("%max%", String.valueOf(max))));
                return;
            }

            manager.setStackCount(against, current + 1);
            if (player.getGameMode() != GameMode.CREATIVE) {
                // Cancelled place events do not consume the item, so consume it ourselves.
                inHand.subtract(1);
            }

            player.playSound(against.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.5f);
            player.sendMessage(mm.deserialize(manager.message("stacked",
                    "<green>Stacked! <gold>%entity% Spawner</gold> is now <yellow><bold>x%count%</bold></yellow>!</green>")
                    .replace("%entity%", SpawnerManager.typeName(itemType))
                    .replace("%count%", String.valueOf(current + 1))));
            return;
        }

        // Fresh spawner: apply type + count once the placement has fully committed.
        EntityType finalType = itemType;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (placed.getType() != Material.SPAWNER) return; // placement was reverted by a later handler
            if (placed.getState() instanceof CreatureSpawner cs) {
                cs.setSpawnedType(finalType);
                cs.update(true, false);
                manager.setStackCount(placed, 1);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        if (!(block.getState() instanceof CreatureSpawner cs)) return;

        Player player = event.getPlayer();
        SpawnerManager manager = manager();

        if (player.getGameMode() != GameMode.CREATIVE && !canHarvest(player)) {
            // Block the break entirely so the stack is not destroyed without a drop.
            event.setCancelled(true);
            return;
        }

        EntityType type = cs.getSpawnedType();
        int stackCount = manager.getStackCount(cs);
        event.setExpToDrop(0);

        if (player.isSneaking() || stackCount <= 1) {
            // Whole stack: let the block break normally; the drop happens at MONITOR once no handler can cancel it.
            return;
        }

        // Mine one from the stack: the block stays, so cancel first, then mutate and drop.
        event.setCancelled(true);
        manager.setStackCount(block, stackCount - 1);
        manager.dropSpawners(block.getLocation(), type, 1);

        player.sendMessage(mm.deserialize(manager.message("mined-stack",
                "<green>You mined 1x from <gold>%entity% Spawner</gold> (%remaining% left in stack).</green>")
                .replace("%entity%", SpawnerManager.typeName(type))
                .replace("%remaining%", String.valueOf(stackCount - 1))));
        player.playSound(block.getLocation(), Sound.BLOCK_METAL_BREAK, 1f, 1f);
    }

    /** Runs only when the break is final. Drops the whole stack exactly once. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakFinal(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        if (!(block.getState() instanceof CreatureSpawner cs)) return;
        if (!event.isDropItems()) return; // another plugin explicitly disabled drops

        Player player = event.getPlayer();
        SpawnerManager manager = manager();
        if (player.getGameMode() != GameMode.CREATIVE && !canHarvest(player)) return;

        EntityType type = cs.getSpawnedType();
        int stackCount = manager.getStackCount(cs);
        manager.dropSpawners(block.getLocation(), type, stackCount);

        player.sendMessage(mm.deserialize(manager.message("mined",
                "<green>You mined a <gold>%entity% Spawner</gold>!</green>")
                .replace("%entity%", SpawnerManager.typeName(type))));
        player.playSound(block.getLocation(), Sound.BLOCK_METAL_BREAK, 1f, 1f);
    }

    /** Silk Touch + minimum pickaxe tier check. Sends the failure message itself. */
    private boolean canHarvest(Player player) {
        if (!plugin.getConfig().getBoolean("require-silk-touch", true)) return true;

        ItemStack tool = player.getInventory().getItemInMainHand();
        String tierName = plugin.getConfig().getString("minimum-pickaxe-tier", "");
        int required = SpawnerManager.requiredTier(tierName);
        int have = SpawnerManager.pickaxeTier(tool.getType().name());

        boolean ok = tool.containsEnchantment(Enchantment.SILK_TOUCH) && (required < 0 || have >= required);
        if (!ok) {
            String def = required < 0
                    ? "<red>You need Silk Touch to mine this spawner!</red>"
                    : "<red>You need a Silk Touch %tier%+ pickaxe to mine this spawner!</red>";
            player.sendMessage(mm.deserialize(manager().message("need-silk-touch", def)
                    .replace("%tier%", SpawnerManager.formatName(tierName))));
        }
        return ok;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = event.getSpawner();
        if (spawner == null) return; // e.g. minecart spawners
        int stackCount = manager().getStackCount(spawner);
        if (stackCount <= 1) return;

        // Extra mobs per wave are capped to keep ticks cheap. Spawned with SPAWNER reason so
        // other plugins treat them like spawner mobs; this does not re-fire SpawnerSpawnEvent.
        int cap = Math.max(0, plugin.getConfig().getInt("max-extra-spawns-per-wave", 4));
        int extraSpawns = Math.min(cap, stackCount - 1);
        for (int i = 0; i < extraSpawns; i++) {
            event.getLocation().getWorld().spawnEntity(event.getLocation(), event.getEntityType(),
                    CreatureSpawnEvent.SpawnReason.SPAWNER);
        }
    }
}
