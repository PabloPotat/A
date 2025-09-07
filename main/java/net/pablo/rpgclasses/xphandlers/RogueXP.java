package net.pablo.rpgclasses.xphandlers;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.pablo.rpgclasses.RpgClassesMod;
import net.pablo.rpgclasses.capability.PlayerClassProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RpgClassesMod.MOD_ID)
public class RogueXP {

    private static final double BASE_GAIN = 5.0;
    private static final double CAP_GAIN = 80.0;
    private static final int CAP_LEVEL = 60;
    private static final double GAIN_MULT = Math.pow(CAP_GAIN / BASE_GAIN, 1.0 / (CAP_LEVEL - 1));
    private static final double SECONDARY_SCALE = 0.5;

    private static final Map<String, Long> lastXpTime = new HashMap<>();
    private static final long COOLDOWN_MS = 1000; // 1 second cooldown

    @SubscribeEvent
    public static void onCritHit(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        UUID targetUUID = event.getEntity().getUUID();
        String key = player.getUUID() + "-" + targetUUID;
        long now = System.currentTimeMillis();
        if (lastXpTime.containsKey(key) && now - lastXpTime.get(key) < COOLDOWN_MS) return;
        lastXpTime.put(key, now);

        // Detect jump crit
        boolean jumpCrit = player.fallDistance > 0.0F
                && !player.onGround()
                && !player.isInWater()
                && !player.isPassenger();

        // Detect backstab (player behind target)
        // Get target direction and vector to player
        Vec3 targetDir = event.getEntity().getLookAngle();
        Vec3 toPlayer = event.getEntity().position().subtract(player.position()).normalize();

    // Flatten vectors on XZ plane
        Vec3 targetDirFlat = new Vec3(targetDir.x, 0, targetDir.z).normalize();
        Vec3 toPlayerFlat = new Vec3(toPlayer.x, 0, toPlayer.z).normalize();

    // Backstab only triggers when player is behind target
        boolean backstab = targetDirFlat.dot(toPlayerFlat) > 0.7; // adjust threshold 0.7–0.9


        // Detect invisibility crit (your passive ensures all hits crit)
        boolean invisCrit = player.hasEffect(MobEffects.INVISIBILITY);

        boolean isCrit = jumpCrit || backstab || invisCrit;
        if (!isCrit) return;

        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).ifPresent(cap -> {
            boolean isRoguePrimary = cap.getSelectedClass() != null &&
                    "Rogue".equalsIgnoreCase(cap.getSelectedClass().getClassName());
            boolean isRogueSecondary = cap.getSecondaryClass() != null &&
                    "Rogue".equalsIgnoreCase(cap.getSecondaryClass().getClassName());

            if (!isRoguePrimary && !isRogueSecondary) return;

            int level = 0;
            if (isRoguePrimary) {
                level = cap.getLevel(cap.getSelectedClass().getClassName());
            } else if (isRogueSecondary) {
                level = cap.getLevel(cap.getSecondaryClass().getClassName());
            }

            double xpToAdd = calculateRogueXP(level);

            if (isRoguePrimary) {
                XPUtils.addXPAndCheckLevel(player, cap, "rogue", (int) xpToAdd);
            }
            if (isRogueSecondary) {
                XPUtils.addXPAndCheckLevel(player, cap, "rogue", (int) (xpToAdd * SECONDARY_SCALE));
            }
        });
    }

    private static double calculateRogueXP(int level) {
        double gain = BASE_GAIN * Math.pow(GAIN_MULT, level - 1);
        return Math.min(gain, CAP_GAIN);
    }
}
