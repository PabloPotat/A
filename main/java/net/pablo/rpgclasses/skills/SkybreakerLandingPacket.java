package net.pablo.rpgclasses.skills;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

import java.util.List;
import java.util.function.Supplier;

// SkybreakerLandingPacket.java
public class SkybreakerLandingPacket {
    public static final ResourceLocation CHANNEL = new ResourceLocation("rpgclasses", "skybreaker_landing");

    public void encode(FriendlyByteBuf buf) {
        // No data needed
    }

    public static SkybreakerLandingPacket decode(FriendlyByteBuf buf) {
        return new SkybreakerLandingPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                handleLandingDamage(player);
            }
        });
        context.get().setPacketHandled(true);
    }

    private void handleLandingDamage(ServerPlayer player) {
        Fighter fighter = getPrimaryFighterClass(player);
        if (fighter == null) return;

        int combo = fighter.getCombo(player.getUUID());
        double damage = 5.0 + combo + fighter.getBaseAttack() * 0.1;

        Level world = player.level();
        AABB area = player.getBoundingBox().inflate(5.0);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            // Apply damage
            target.hurt(player.damageSources().playerAttack(player), (float) damage);

            // Apply stun with dizzy effect
            double stunSeconds = Math.min(2.0 + (0.5 * combo), 4.0);
            applyStunWithDizzyEffect(target, (float) stunSeconds, player);
        }

        // Set cooldown
        CooldownManager.setCooldown(player);

        // Landing sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void applyStunWithDizzyEffect(LivingEntity target, float stunSeconds, Player source) {
        // Launch the target into the air first
        target.setDeltaMovement(0, 0.5, 0); // Launch up 1 block high
        target.hurtMarked = false; // Prevent knockback from other sources

        // Apply stun effects after a short delay to allow the launch
        target.getServer().execute(() -> {
            if (target.isAlive()) {
                // Strong vanilla effects for complete stun
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int)(stunSeconds * 20), 255, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, (int)(stunSeconds * 20), 255, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int)(stunSeconds * 20), 255, false, true));

                // Completely stop any remaining movement
                target.setDeltaMovement(0, 0, 0);
                target.hurtMarked = true;

                // Create star halo effect
                createDizzyEffect(target, stunSeconds);

                // Launch sound effect
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.7f, 1.2f);
            }
        });
    }

    private static void createDizzyEffect(LivingEntity target, float stunSeconds) {
        Level world = target.level();

        // Simple immediate particles without scheduling
        if (world instanceof ServerLevel serverLevel) {
            // Create rotating star halo above the target's head
            createStarHalo(serverLevel, target, stunSeconds);
        }
    }

    private static void createStarHalo(ServerLevel serverLevel, LivingEntity target, float duration) {
        double centerX = target.getX();
        double centerY = target.getY() + target.getBbHeight() + 0.8; // Above head
        double centerZ = target.getZ();
        double radius = 0.6;

        // Create a circle of different star particles
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6; // 30 degree increments
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;

            // Alternate between different star-like particles
            ParticleOptions particle;
            if (i % 3 == 0) {
                particle = ParticleTypes.CRIT; // Yellow stars
            } else if (i % 3 == 1) {
                particle = ParticleTypes.ENCHANTED_HIT; // Magical blue stars
            } else {
                particle = ParticleTypes.ELECTRIC_SPARK; // Sparkly stars
            }

            serverLevel.sendParticles(particle, x, centerY, z, 1, 0, 0, 0, 0.1);
        }

        // Add some floating particles in the center
        serverLevel.sendParticles(ParticleTypes.GLOW, centerX, centerY, centerZ, 3, 0.2, 0.2, 0.2, 0.05);

        // Schedule rotating animation
        for (int tick = 0; tick < (int)(duration * 20); tick += 2) { // Every 2 ticks
            int finalTick = tick;
            serverLevel.getServer().execute(() -> {
                if (target.isAlive() && target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                    createRotatingStarHalo(serverLevel, target, finalTick);
                }
            });
        }
    }

    private static void createRotatingStarHalo(ServerLevel serverLevel, LivingEntity target, int tick) {
        double centerX = target.getX();
        double centerY = target.getY() + target.getBbHeight() + 0.8;
        double centerZ = target.getZ();
        double radius = 0.6;

        // Calculate rotation angle based on tick count
        double rotationAngle = (tick * 10) % 360 * Math.PI / 180.0;

        // Create rotating star ring
        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.PI / 4) + rotationAngle;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;

            // Use different particles for variety
            ParticleOptions particle;
            if (i % 4 == 0) {
                particle = ParticleTypes.CRIT;
            } else if (i % 4 == 1) {
                particle = ParticleTypes.ENCHANTED_HIT;
            } else if (i % 4 == 2) {
                particle = ParticleTypes.ELECTRIC_SPARK;
            } else {
                particle = ParticleTypes.WAX_OFF;
            }

            serverLevel.sendParticles(particle, x, centerY, z, 1, 0, 0, 0, 0.1);
        }

        // Pulsing center glow
        double pulse = 0.5 + 0.3 * Math.sin(tick * 0.2);
        serverLevel.sendParticles(ParticleTypes.GLOW, centerX, centerY, centerZ, 2, pulse * 0.1, pulse * 0.1, pulse * 0.1, 0.03);
    }

    private Fighter getPrimaryFighterClass(Player player) {
        var cap = player.getCapability(net.pablo.rpgclasses.capability.PlayerClassProvider.PLAYER_CLASS_CAPABILITY).orElse(null);
        if (cap == null) return null;
        if (cap.getSelectedClass() instanceof Fighter fPrimary) return fPrimary;
        return null;
    }
}
