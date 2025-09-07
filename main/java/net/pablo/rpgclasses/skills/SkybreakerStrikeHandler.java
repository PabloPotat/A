package net.pablo.rpgclasses.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.pablo.rpgclasses.NetworkHandler;
import net.pablo.rpgclasses.keybinds.KeyBindings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// SkybreakerStrikeHandler.java
public class SkybreakerStrikeHandler {

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            while (KeyBindings.SKYBREAKER_STRIKE.consumeClick()) {
                if (Minecraft.getInstance().player != null) {
                    Player player = Minecraft.getInstance().player;

                    // Only allow if on ground and not on cooldown
                    if (player.onGround() && !CooldownManager.isOnCooldown(player)) {
                        // Send packet to server to start jump
                        NetworkHandler.INSTANCE.sendToServer(new SkybreakerStrikePacket());

                        // Client-side jump effects only
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.8, player.getDeltaMovement().z);

                        for (int i = 0; i < 5; i++) {
                            player.level().addParticle(ParticleTypes.CLOUD,
                                    player.getX(), player.getY(), player.getZ(),
                                    0, 0.1, 0);
                        }

                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.8f);
                    }
                }
            }
        }
    }
}