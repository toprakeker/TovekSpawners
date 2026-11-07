package dev.toprak.spawners;

import dev.toprak.spawners.listener.SpawnerListener;
import dev.toprak.spawners.manager.SpawnerManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlacementProtectionTest {
    private final SpawnersPlugin plugin = mock(SpawnersPlugin.class);
    private final SpawnerManager manager = mock(SpawnerManager.class);
    private final Block placed = mock(Block.class);
    private final Block against = mock(Block.class);
    private final CreatureSpawner target = mock(CreatureSpawner.class);
    private final Player player = mock(Player.class);
    private final ItemStack item = mock(ItemStack.class);

    private BlockPlaceEvent placement(boolean canBuild) {
        when(plugin.getSpawnerManager()).thenReturn(manager);
        when(placed.getType()).thenReturn(Material.SPAWNER);
        when(against.getType()).thenReturn(Material.SPAWNER);
        when(against.getState()).thenReturn(target);
        when(target.getSpawnedType()).thenReturn(EntityType.ZOMBIE);
        when(item.getType()).thenReturn(Material.SPAWNER);
        when(manager.getEntityTypeFromItem(item)).thenReturn(EntityType.ZOMBIE);
        when(manager.getStackCount(target)).thenReturn(3);
        when(manager.getMaxStackSize()).thenReturn(50);
        when(manager.message(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        return new BlockPlaceEvent(placed, mock(BlockState.class), against, item, player, canBuild);
    }

    @Test
    void buildDeniedPlacementCannotModifyAnExistingSpawner() {
        BlockPlaceEvent event = placement(false);
        assertFalse(event.isCancelled(), "Build denial is independent from cancellation");
        assertFalse(event.canBuild());
        new SpawnerListener(plugin).onPlace(event);

        verifyNoInteractions(manager, item, player);
        verify(plugin, never()).getServer();
    }

    @Test
    void buildDeniedFreshPlacementCannotScheduleWorldChanges() {
        BlockPlaceEvent event = placement(false);
        when(against.getType()).thenReturn(Material.STONE);
        new SpawnerListener(plugin).onPlace(event);
        verifyNoInteractions(manager, item, player);
        verify(plugin, never()).getServer();
    }

    @Test
    void cancelledPlacementCannotStackEvenWhenBuildFlagAllowsIt() {
        BlockPlaceEvent event = placement(true);
        event.setCancelled(true);
        new SpawnerListener(plugin).onPlace(event);
        verifyNoInteractions(manager, item, player);
    }

    @Test
    void permittedPlacementStillStacksAndConsumesOneItem() {
        BlockPlaceEvent event = placement(true);
        new SpawnerListener(plugin).onPlace(event);
        verify(manager).setStackCount(against, 4);
        verify(item).subtract(1);
        assertTrue(event.isCancelled(), "Stacking must not also place a new block");
    }

    @Test
    void laterBuildDenialPreventsDeferredFreshPlacementMutation() {
        assertDeferredMutationRejected(false);
    }

    @Test
    void laterCancellationPreventsDeferredFreshPlacementMutation() {
        assertDeferredMutationRejected(true);
    }

    private void assertDeferredMutationRejected(boolean cancel) {
        BlockPlaceEvent event = placement(true);
        when(against.getType()).thenReturn(Material.STONE);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        new SpawnerListener(plugin).onPlace(event);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), task.capture());
        if (cancel) event.setCancelled(true);
        else event.setBuild(false);
        clearInvocations(placed, manager);
        task.getValue().run();
        verifyNoInteractions(placed, manager);
    }
}
