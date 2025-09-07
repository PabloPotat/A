package net.pablo.rpgclasses.classes;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Fighter extends RPGClass {

    private static final double DEFAULT_HEALTH = 30.0;
    private static final double DEFAULT_SPEED = 0.1;
    private static final double DEFAULT_ATTACK = 4.0;

    private static final long COMBO_TIMEOUT_MS = 5000;      // 5 sec hit timeout
    private static final long STAND_STILL_GRACE_MS = 1000;  // 2 sec grace before reset
    private static final double MIN_MOVE_DISTANCE = 0.1;    // movement threshold

    // Player-specific maps
    private final Map<UUID, Integer> comboMap = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, double[]> lastPosition = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();

    public Fighter() {
        super("fighter", DEFAULT_HEALTH, DEFAULT_SPEED, DEFAULT_ATTACK);
    }

    /** Check if player moved enough since last check, update lastMoveTime */
    public boolean hasMovedEnough(Player player) {
        double[] lastPos = lastPosition.getOrDefault(player.getUUID(),
                new double[]{player.getX(), player.getY(), player.getZ()});

        double distance = Math.sqrt(
                Math.pow(player.getX() - lastPos[0], 2) +
                        Math.pow(player.getY() - lastPos[1], 2) +
                        Math.pow(player.getZ() - lastPos[2], 2)
        );

        lastPosition.put(player.getUUID(), new double[]{player.getX(), player.getY(), player.getZ()});

        if (distance >= MIN_MOVE_DISTANCE) {
            lastMoveTime.put(player.getUUID(), System.currentTimeMillis());
            return true;
        }
        return false;
    }

    /** Get current combo count */
    public int getCombo(UUID playerId) {
        return comboMap.getOrDefault(playerId, 0);
    }

    /** Increase combo on hit */
    public void increaseCombo(UUID playerId, Player player) {
        if (!hasMovedEnough(player)) return; // Only increase if player moved

        int combo = getCombo(playerId) + 1;
        comboMap.put(playerId, combo);
        lastHitTime.put(playerId, System.currentTimeMillis());
    }

    /** Reset combo if player inactive or standing still for grace period */
    public void resetComboIfTimedOut(UUID playerId, Player player) {
        long now = System.currentTimeMillis();

        // Reset if player hasn’t moved enough for grace period
        long lastMove = lastMoveTime.getOrDefault(playerId, now);
        if (now - lastMove > STAND_STILL_GRACE_MS) {
            comboMap.put(playerId, 0);
            return;
        }

        // Reset if player hasn’t hit for COMBO_TIMEOUT_MS
        long lastHit = lastHitTime.getOrDefault(playerId, 0L);
        if (now - lastHit > COMBO_TIMEOUT_MS) {
            comboMap.put(playerId, 0);
        }
    }

    /** Return base attack stat */
    public double getBaseAttack() {
        return getAttackDamage();
    }

    @Override
    public void applyClassEffect(Player player) {
        // Keep your existing attribute + effect code here
    }

    @Override
    public void removeClassEffect(Player player) {
        // Keep your existing remove code here
    }

    /** Get current combo count for display */
    public int getCurrentCombo(UUID playerId, Player player) {
        resetComboIfTimedOut(playerId, player); // Check if combo should reset
        return comboMap.getOrDefault(playerId, 0);
    }
}
