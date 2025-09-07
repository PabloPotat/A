package net.pablo.rpgclasses.skills;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// CooldownManager.java
// In CooldownManager.java
public class CooldownManager {
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    public static boolean isOnCooldown(Player player) {
        return getCooldown(player) > 0;
    }

    public static double getCooldown(Player player) {
        Long lastUsed = cooldowns.get(player.getUUID());
        if (lastUsed == null) return 0;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastUsed;
        double remaining = Math.max(0, 10000 - elapsed) / 1000.0; // 10 second cooldown

        return remaining;
    }

    public static void setCooldown(Player player) {
        cooldowns.put(player.getUUID(), System.currentTimeMillis());
    }

    // ADD THIS METHOD
    public static void refundCooldown(Player player) {
        Long lastUsed = cooldowns.get(player.getUUID());
        if (lastUsed != null) {
            // Refund 50% of cooldown time
            long refundAmount = 5000; // 5 seconds (50% of 10 seconds)
            long newCooldownTime = System.currentTimeMillis() - refundAmount;
            cooldowns.put(player.getUUID(), newCooldownTime);
        }
    }
}