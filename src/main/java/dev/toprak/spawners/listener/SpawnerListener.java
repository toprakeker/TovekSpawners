package dev.toprak.spawners.listener;

import dev.toprak.spawners.SpawnersPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerListener implements Listener {
    private final SpawnersPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SpawnerListener(SpawnersPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        ItemStack inHand = event.getItemInHand();
        EntityType itemType = plugin.getSpawnerManager().getEntityTypeFromItem(inHand);
        if (itemType == null) itemType = EntityType.PIG;

        Block clicked = event.getBlockAgainst();
        if (clicked != null && clicked.getType() == Material.SPAWNER && clicked.getState() instanceof CreatureSpawner targetCs) {
            if (targetCs.getSpawnedType() == itemType) {
                int current = plugin.getSpawnerManager().getStackCount(clicked);
                int max = plugin.getConfig().getInt("max-stack-size", 50);

                if (current >= max) {
                    player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.max-stacked", "")
                            .replace("%max%", String.valueOf(max))));
                    event.setCancelled(true);
                    return;
                }

                plugin.getSpawnerManager().setStackCount(clicked, current + 1);
                event.setCancelled(true);
                inHand.subtract(1);

                player.playSound(clicked.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.5f);
                player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.stacked", "")
                        .replace("%entity%", plugin.getSpawnerManager().formatName(itemType.name()))
                        .replace("%count%", String.valueOf(current + 1))));
                return;
            }
        }

        // Fresh spawner placed
        EntityType finalType = itemType;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (placed.getState() instanceof CreatureSpawner cs) {
                cs.setSpawnedType(finalType);
                cs.update(true, false);
                plugin.getSpawnerManager().setStackCount(placed, 1);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        boolean requireSilk = plugin.getConfig().getBoolean("require-silk-touch", true);
        boolean hasSilk = tool.containsEnchantment(Enchantment.SILK_TOUCH);

        if (requireSilk && !hasSilk) {
            return;
        }

        if (!(block.getState() instanceof CreatureSpawner cs)) return;
        EntityType type = cs.getSpawnedType();
        int stackCount = plugin.getSpawnerManager().getStackCount(block);

        event.setExpToDrop(0);

        if (player.isSneaking() || stackCount <= 1) {
            // Mine entire stack
            ItemStack drop = plugin.getSpawnerManager().createSpawnerItem(type, stackCount);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);

            player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.mined", "")
                    .replace("%entity%", plugin.getSpawnerManager().formatName(type.name()))));
        } else {
            // Mine 1 from stack
            plugin.getSpawnerManager().setStackCount(block, stackCount - 1);
            event.setCancelled(true);

            ItemStack drop = plugin.getSpawnerManager().createSpawnerItem(type, 1);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);

            player.sendMessage(mm.deserialize(plugin.getConfig().getString("messages.mined-stack", "")
                    .replace("%entity%", plugin.getSpawnerManager().formatName(type.name()))
                    .replace("%remaining%", String.valueOf(stackCount - 1))));
        }

        player.playSound(block.getLocation(), Sound.BLOCK_METAL_BREAK, 1f, 1f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = event.getSpawner();
        int stackCount = plugin.getSpawnerManager().getStackCount(spawner.getBlock());
        if (stackCount <= 1) return;

        // Multiply spawn count safely (cap at 4 extra per wave to prevent tick lag)
        int extraSpawns = Math.min(4, stackCount - 1);
        for (int i = 0; i < extraSpawns; i++) {
            event.getEntity().getWorld().spawnEntity(event.getLocation(), event.getEntityType());
        }
    }
}
