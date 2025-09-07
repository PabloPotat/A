package net.pablo.rpgclasses.skills;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import net.pablo.rpgclasses.classes.Fighter;
import net.pablo.rpgclasses.effects.ModEffects;

import java.util.List;
import java.util.function.Supplier;

// SkybreakerStrikePacket.java
public class SkybreakerStrikePacket {
    public static final ResourceLocation CHANNEL = new ResourceLocation("rpgclasses", "skybreaker_strike");

    public void encode(FriendlyByteBuf buf) {
        // No data needed
    }

    public static SkybreakerStrikePacket decode(FriendlyByteBuf buf) {
        return new SkybreakerStrikePacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                // Start tracking the jump on server side
                SkybreakerStrikeManager.startJump(player);

                // Server-side jump
                player.setDeltaMovement(player.getDeltaMovement().x, 0.8, player.getDeltaMovement().z);

                // Server-side jump sound
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
        });
        context.get().setPacketHandled(true);
    }
}