package net.pablo.rpgclasses.xphandlers;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.pablo.rpgclasses.RpgClassesMod;
import net.pablo.rpgclasses.capability.PlayerClassProvider;
import net.pablo.rpgclasses.classes.Fighter;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = RpgClassesMod.MOD_ID)
public class FighterXP {

    private static final int XP_CAP = 50;
    private static final double SECONDARY_SCALE = 0.6;
    private static final double STREAK_MULTIPLIER = 0.15; // combo multiplier

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).ifPresent(cap -> {

            // Determine Fighter instance
            Fighter fighter = null;
            boolean isPrimary = false;
            boolean isSecondary = false;

            if (cap.getSelectedClass() instanceof Fighter fPrimary) {
                fighter = fPrimary;
                isPrimary = true;
            } else if (cap.getSecondaryClass() instanceof Fighter fSecondary) {
                fighter = fSecondary;
                isSecondary = true;
            }

            if (fighter == null) return;

            // Reset combo if timed out or standing still
            fighter.resetComboIfTimedOut(player.getUUID(), player);

            // Increase combo for this hit only if player moved
            fighter.increaseCombo(player.getUUID(), player);
            int combo = fighter.getCombo(player.getUUID());

            // Only award XP if combo > 0
            if (combo > 0) {
                double level = cap.getLevel("fighter");
                double baseXP = 2 + level * 0.8;

                double xpFromCombo = baseXP * (1.0 + STREAK_MULTIPLIER * (combo - 1));
                int xpToAward = (int) Math.min(xpFromCombo, XP_CAP);

                // Award XP to primary
                if (isPrimary) {
                    XPUtils.addXPAndCheckLevel(player, cap, "fighter", xpToAward);
                }

                // Award scaled XP to secondary
                if (isSecondary) {
                    int secXP = (int) Math.round(xpToAward * SECONDARY_SCALE);
                    XPUtils.addXPAndCheckLevel(player, cap, "fighter", secXP);
                }
            }
        });
    }
}
