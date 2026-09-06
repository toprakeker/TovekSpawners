package dev.toprak.spawners.command;

import dev.toprak.spawners.SpawnersPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnerCommand implements CommandExecutor, TabCompleter {
    private final SpawnersPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SpawnerCommand(SpawnersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("spawners.admin")) {
            sender.sendMessage(mm.deserialize("<red>You do not have permission.</red>"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(mm.deserialize("<yellow>/spawners give <player> <type> [amount]</yellow>"));
            sender.sendMessage(mm.deserialize("<yellow>/spawners reload</yellow>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(mm.deserialize("<green>TovekSpawners configuration reloaded.</green>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage(mm.deserialize("<red>Usage: /spawners give <player> <entityType> [amount]</red>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>Player not found.</red>"));
                return true;
            }

            EntityType type;
            try {
                type = EntityType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(mm.deserialize("<red>Invalid entity type.</red>"));
                return true;
            }

            int amount = 1;
            if (args.length > 3) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {}
            }

            ItemStack spawnerItem = plugin.getSpawnerManager().createSpawnerItem(type, amount);
            var leftover = target.getInventory().addItem(spawnerItem);
            leftover.values().forEach(it -> target.getWorld().dropItemNaturally(target.getLocation(), it));

            sender.sendMessage(mm.deserialize("<green>Gave " + amount + "x " + plugin.getSpawnerManager().formatName(type.name()) + " Spawner to " + target.getName() + ".</green>"));
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(EntityType.values())
                    .filter(EntityType::isAlive)
                    .map(Enum::name)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .limit(20)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
