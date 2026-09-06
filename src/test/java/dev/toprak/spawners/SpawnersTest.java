package dev.toprak.spawners;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpawnersTest {

    @Test
    void testFormatName() {
        String name = "ZOMBIE_VILLAGER";
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1).toLowerCase())
              .append(" ");
        }
        assertEquals("Zombie Villager", sb.toString().trim());
    }

    @Test
    void testStackLimits() {
        int max = 50;
        int current = 49;
        assertTrue(current + 1 <= max);
        assertFalse(current + 2 <= max);
    }
}
