package dev.toprak.spawners;

import dev.toprak.spawners.command.SpawnerCommand;
import dev.toprak.spawners.listener.SpawnerListener;
import dev.toprak.spawners.manager.SpawnerManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnersPlugin extends JavaPlugin {
    private static SpawnersPlugin instance;
    private SpawnerManager spawnerManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.spawnerManager = new SpawnerManager(this);
        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);

        PluginCommand cmd = getCommand("spawners");
        if (cmd != null) {
            SpawnerCommand executor = new SpawnerCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("TovekSpawners v" + getPluginMeta().getVersion() + " loaded.");
    }

    public static SpawnersPlugin getInstance() {
        return instance;
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }
}
