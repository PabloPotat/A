package net.pablo.rpgclasses.xphandlers;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.pablo.rpgclasses.RpgClassesMod;
import net.pablo.rpgclasses.capability.PlayerClassProvider;

@Mod.EventBusSubscriber(modid = RpgClassesMod.MOD_ID)
public class MageXP {

    private static final double BASE_GAIN = 5.0;
    private static final double CAP_GAIN = 50.0;
    private static final int CAP_LEVEL = 50;
    private static final double GAIN_MULT = Math.pow(CAP_GAIN / BASE_GAIN, 1.0 / (CAP_LEVEL - 1));
    private static final double SECONDARY_SCALE = 0.5;

    @SubscribeEvent
    public static void onMageCastSpell(SpellOnCastEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) return;

        player.getCapability(PlayerClassProvider.PLAYER_CLASS_CAPABILITY).ifPresent(cap -> {
            boolean isMagePrimary = cap.getSelectedClass() != null &&
                    "Mage".equalsIgnoreCase(cap.getSelectedClass().getClassName());
            boolean isMageSecondary = cap.getSecondaryClass() != null &&
                    "Mage".equalsIgnoreCase(cap.getSecondaryClass().getClassName());

            if (!isMagePrimary && !isMageSecondary) return;

            // Always pull Mage level directly from capability
            int mageLevel = cap.getLevel("mage");
            double xpToAdd = calculateMageXP(mageLevel);

            if (isMagePrimary) {
                XPUtils.addXPAndCheckLevel(player, cap, "mage", (int) xpToAdd);
            }
            if (isMageSecondary) {
                int secXp = (int) Math.round(xpToAdd * SECONDARY_SCALE);
                XPUtils.addXPAndCheckLevel(player, cap, "mage", secXp);
            }
        });
    }

    private static double calculateMageXP(int level) {
        double gain = BASE_GAIN * Math.pow(GAIN_MULT, level - 1);
        return Math.min(gain, CAP_GAIN);
    }
}
