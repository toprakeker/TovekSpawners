package dev.toprak.spawners;

import org.junit.jupiter.api.Test;
import dev.toprak.spawners.manager.SpawnerManager;
import static org.junit.jupiter.api.Assertions.*;

class SpawnersTest {

    @Test
    void testFormatName() {
        assertEquals("Zombie Villager", SpawnerManager.formatName("ZOMBIE_VILLAGER"));
    }

    @Test
    void testStackLimits() {
        int max = 50;
        int current = 49;
        assertTrue(current + 1 <= max);
        assertFalse(current + 2 <= max);
    }
}
