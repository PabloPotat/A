package net.pablo.rpgclasses.xphandlers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.pablo.rpgclasses.RpgClassesMod;
import net.pablo.rpgclasses.capability.PlayerClassProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RpgClassesMod.MOD_ID)
public class TankXP {

    private static final double SECONDARY_SCALE = 0.6;     // Secondary class XP scale
    private static final long AFK_TIMEOUT = 10_000;        // 10 seconds
    private static final int BASE_XP = 1;                  // XP at level 1
    private static final int XP_CAP = 55;                  // Max XP per hit
    private static final double MIN_HIT_XP_RATIO = 0.15;   // 15% XP for weak hits

    private static final Map<UUID, Long> lastAttack = new HashMap<>();
    private static final Map<UUID, Long> lastMove = new HashMap<>();
    private static final Map<UUID, Vec3> lastPos = new HashMap<>();

    // --- Award XP when player is hit ---
    @SubscribeEvent
    public static void onHitTaken(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).ifPresent(cap -> {
            boolean isPrimary = cap.getSelectedClass() != null &&
                    "Tank".equalsIgnoreCase(cap.getSelectedClass().getClassName());
            boolean isSecondary = cap.getSecondaryClass() != null &&
                    "Tank".equalsIgnoreCase(cap.getSecondaryClass().getClassName());

            if (!isPrimary && !isSecondary) return;

            UUID id = player.getUUID();
            long now = System.currentTimeMillis();

            // --- AFK prevention: must have moved AND attacked in last 10s ---
            boolean activeMove = lastMove.containsKey(id) && now - lastMove.get(id) < AFK_TIMEOUT;
            boolean activeAttack = lastAttack.containsKey(id) && now - lastAttack.get(id) < AFK_TIMEOUT;
            if (!(activeMove && activeAttack)) return;

            int level = cap.getLevel("tank");
            if (level < 1) level = 1;

            // --- Minimum damage scaling with cap at level 75 ---
            double minDamage = 0.5 + (level * 0.05);
            if (level >= 75) minDamage = 10;

            double damageTaken = event.getAmount();
            if (damageTaken <= 0) return;  // ignore fully mitigated hits

            // --- XP scaling per hit (exponential, capped at XP_CAP) ---
            double xp = BASE_XP * Math.pow(1.08, level - 1);
            if (xp > XP_CAP) xp = XP_CAP;

            int xpToAward;
            // Only give weak XP if the hit is at least half the minDamage
            if (damageTaken >= minDamage || damageTaken >= minDamage / 2) {
                xpToAward = (int) Math.round(xp);  // full XP
            } else {
                xpToAward = 0;  // ignore tiny hits completely
            }

            // --- Award XP ---
            if (xpToAward > 0) {
                if (isPrimary) {
                    XPUtils.addXPAndCheckLevel(player, cap, "Tank", xpToAward);
                }
                if (isSecondary) {
                    int secXp = (int) Math.round(xpToAward * SECONDARY_SCALE);
                    XPUtils.addXPAndCheckLevel(player, cap, "Tank", secXp);
                }
            }
        });
    }

    // --- Track player attacking mobs ---
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        lastAttack.put(player.getUUID(), System.currentTimeMillis());
    }

    // --- Track player movement ---
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide) return;

        UUID id = player.getUUID();
        Vec3 currentPos = player.position();

        if (!lastPos.containsKey(id) || !lastPos.get(id).equals(currentPos)) {
            lastPos.put(id, currentPos);
            lastMove.put(id, System.currentTimeMillis());
        }
    }
}
