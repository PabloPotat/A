package net.pablo.rpgclasses.xphandlers;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.pablo.rpgclasses.capability.IPlayerClass;

import java.util.HashMap;
import java.util.Map;

public class XPUtils {

    // Diminishing maps for mob kills
    public static final Map<String, Double> entityDiminish = new HashMap<>();
    public static final Map<String, Long> lastDecayTime = new HashMap<>();
    public static final long DECAY_INTERVAL_MS = 60000; // 1 minute

    /** Add XP and notify player */
    public static void addXPAndCheckLevel(Player player, IPlayerClass cap, String className, int xpAmount) {
        if (player == null || cap == null || className == null) return;

        // Normalize class name key
        String key = className.toLowerCase();

        // Get old level before adding XP
        int oldLevel = cap.getLevel(key);

        // Add XP
        cap.addXP(key, xpAmount);

        // Retrieve updated XP and level
        int newLevel = cap.getLevel(key);
        int newXP = cap.getXP(key);

        // Notify player of XP gain
        player.sendSystemMessage(Component.literal(
                "Gained " + xpAmount + " XP for " + className +
                        " (Level: " + newLevel + ", XP: " + newXP + ")"
        ));

        // Notify player if they leveled up
        if (newLevel > oldLevel) {
            player.sendSystemMessage(Component.literal(
                    "Your " + className + " class leveled up to " + newLevel + "!"
            ));
        }
    }


    /** Decay diminishing returns over time */
    public static void decayDiminishing() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastDecayTime.entrySet()) {
            String key = entry.getKey();
            long last = entry.getValue();
            if (now - last >= DECAY_INTERVAL_MS) {
                entityDiminish.put(key, Math.min(1.0, entityDiminish.getOrDefault(key, 1.0) + 0.05));
                lastDecayTime.put(key, now);
            }
        }
    }
}
