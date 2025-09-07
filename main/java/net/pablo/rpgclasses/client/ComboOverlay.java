// ComboOverlay.java
package net.pablo.rpgclasses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.pablo.rpgclasses.classes.Fighter;

public class ComboOverlay {
    private static final ResourceLocation COMBO_ICON = new ResourceLocation("rpgclasses", "textures/gui/combo_icon.png");

    public static final IGuiOverlay COMBO_OVERLAY = ComboOverlay::renderComboCounter;

    private static void renderComboCounter(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        // Use your capability system
        Fighter fighter = getPrimaryFighterClass(minecraft.player);
        if (fighter == null) return;

        int combo = fighter.getCurrentCombo(minecraft.player.getUUID(), minecraft.player);
        if (combo <= 0) return;

        int x = width / 2 + 50;
        int y = height / 2 - 30;

        // Draw combo icon (make sure you have this texture in your resources)
        guiGraphics.blit(COMBO_ICON, x, y, 0, 0, 16, 16, 16, 16);

        // Draw combo count
        guiGraphics.drawString(minecraft.font, String.valueOf(combo), x + 20, y + 4, 0xFFFFFF);

        // Optional: Draw combo multiplier text
        if (combo > 1) {
            double multiplier = 1.0 + (Math.min(combo, 10) * 0.05);
            String multiplierText = String.format("x%.1f", multiplier);
            guiGraphics.drawString(minecraft.font, multiplierText, x + 20, y + 16, 0x00FF00);
        }
    }
    private static Fighter getPrimaryFighterClass(Player player) {
        var cap = player.getCapability(net.pablo.rpgclasses.capability.PlayerClassProvider.PLAYER_CLASS_CAPABILITY).orElse(null);
        if (cap == null) return null;
        if (cap.getSelectedClass() instanceof Fighter fPrimary) return fPrimary;
        return null;
    }
}