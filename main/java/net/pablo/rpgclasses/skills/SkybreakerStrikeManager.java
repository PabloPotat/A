package net.pablo.rpgclasses.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.pablo.rpgclasses.classes.Fighter;

import java.util.*;

import static net.pablo.rpgclasses.skills.CooldownManager.refundCooldown;

public class SkybreakerStrikeManager {
    private static final Map<UUID, Boolean> jumpingPlayers = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    private static final Map<UUID, DelayedStunInfo> delayedStuns = new HashMap<>();

    private static class DelayedStunInfo {
        public final float stunSeconds;
        public final Player source;
        public int ticksRemaining;

        public DelayedStunInfo(float stunSeconds, Player source, int delayTicks) {
            this.stunSeconds = stunSeconds;
            this.source = source;
            this.ticksRemaining = delayTicks;
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (UUID playerId : new HashSet<>(jumpingPlayers.keySet())) {
                ServerPlayer player = getServerPlayer(playerId);
                if (player != null && jumpingPlayers.get(playerId)) {
                    checkLanding(player);
                }
            }
            handleDelayedStuns();
        }
    }

    private static void checkLanding(ServerPlayer player) {
        UUID playerId = player.getUUID();
        boolean currentlyOnGround = player.onGround();
        boolean previouslyOnGround = wasOnGround.getOrDefault(playerId, false);

        if (!previouslyOnGround && currentlyOnGround) {
            handleLandingDamage(player);
            jumpingPlayers.put(playerId, false);
        }
        wasOnGround.put(playerId, currentlyOnGround);
    }

    public static void startJump(ServerPlayer player) {
        jumpingPlayers.put(player.getUUID(), true);
        wasOnGround.put(player.getUUID(), true);
    }

    private static void handleLandingDamage(ServerPlayer player) {
        Fighter fighter = getPrimaryFighterClass(player);
        if (fighter == null) return;

        player.fallDistance = 0;
        int combo = fighter.getCombo(player.getUUID());
        double damage = 5.0 + combo + fighter.getBaseAttack() * 0.1;

        Level world = player.level();
        AABB area = player.getBoundingBox().inflate(5.0);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());

        // Apply damage to all targets simultaneously
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), (float) damage);
        }

        // Apply stun to all targets simultaneously
        double stunSeconds = Math.min(2.0 + (0.5 * combo), 4.0);
        for (LivingEntity target : targets) {
            applyStunWithDizzyEffect(target, (float) stunSeconds, player);
        }

        // Create particles for all targets
        createSimultaneousParticles(targets, stunSeconds);

        if (targets.isEmpty()) {
            refundCooldown(player);
        } else {
            CooldownManager.setCooldown(player);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
        createLandingEffects(player);
    }

    private static void applyStunWithDizzyEffect(LivingEntity target, float stunSeconds, Player source) {
        // Strong launch with increased force
        target.setDeltaMovement(target.getDeltaMovement().x, 0.8, target.getDeltaMovement().z);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SLIME_JUMP, SoundSource.NEUTRAL, 0.8f, 0.6f);
        addLaunchParticles(target);

        // Schedule stun for 2 ticks later to allow launch to complete
        delayedStuns.put(target.getUUID(), new DelayedStunInfo(stunSeconds, source, 2));
    }

    private static void handleDelayedStuns() {
        Iterator<Map.Entry<UUID, DelayedStunInfo>> iterator = delayedStuns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DelayedStunInfo> entry = iterator.next();
            DelayedStunInfo info = entry.getValue();
            info.ticksRemaining--;

            if (info.ticksRemaining <= 0) {
                iterator.remove();
                LivingEntity target = findEntityById(entry.getKey());
                if (target != null && target.isAlive()) {
                    applyFinalStunEffects(target, info.stunSeconds, info.source);
                }
            }
        }
    }

    private static void applyFinalStunEffects(LivingEntity target, float stunSeconds, Player source) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int)(stunSeconds * 20), 255, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, (int)(stunSeconds * 20), 255, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int)(stunSeconds * 20), 255, false, true));
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        // FIXED: Call the correct method with ServerLevel
        Level world = target.level();
        if (world instanceof ServerLevel serverLevel) {
            createDizzyEffect(serverLevel, target, stunSeconds);
        }

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5f, 1.5f);
    }

    private static void createSimultaneousParticles(List<LivingEntity> targets, double stunSeconds) {
        for (LivingEntity target : targets) {
            Level world = target.level();
            if (world instanceof ServerLevel serverLevel) {
                createDizzyEffect(serverLevel, target, stunSeconds); // FIXED: Added serverLevel parameter
            }
        }
    }

    private static void createDizzyEffect(ServerLevel serverLevel, LivingEntity target, double duration) {
        double centerX = target.getX();
        double centerY = target.getY() + target.getBbHeight() + 0.8;
        double centerZ = target.getZ();
        double radius = 0.6;

        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;

            ParticleOptions particle = switch (i % 3) {
                case 0 -> ParticleTypes.CRIT;
                case 1 -> ParticleTypes.ENCHANTED_HIT;
                default -> ParticleTypes.ELECTRIC_SPARK;
            };

            serverLevel.sendParticles(particle, x, centerY, z, 1, 0, 0, 0, 0.1);
        }
        serverLevel.sendParticles(ParticleTypes.GLOW, centerX, centerY, centerZ, 3, 0.2, 0.2, 0.2, 0.05);
    }

    private static void addLaunchParticles(LivingEntity target) {
        Level world = target.level();
        if (world instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                double radius = 0.4;
                double x = target.getX() + Math.cos(angle) * radius;
                double z = target.getZ() + Math.sin(angle) * radius;
                serverLevel.sendParticles(ParticleTypes.CLOUD, x, target.getY(), z, 2, 0, 0.2, 0, 0.1);
            }
            serverLevel.sendParticles(ParticleTypes.POOF, target.getX(), target.getY(), target.getZ(), 8, 0.3, 0.2, 0.3, 0.1);
        }
    }

    private static void createLandingEffects(Player player) {
        Level world = player.level();
        if (world instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double angle = world.random.nextDouble() * Math.PI * 2;
                double distance = world.random.nextDouble() * 3.0;
                double x = player.getX() + Math.cos(angle) * distance;
                double z = player.getZ() + Math.sin(angle) * distance;
                serverLevel.sendParticles(ParticleTypes.CLOUD, x, player.getY(), z, 1, 0, 0.1, 0, 0.1);
                serverLevel.sendParticles(ParticleTypes.CRIT, x, player.getY() + 0.1, z, 1, 0, 0.1, 0, 0.1);
            }
        }
    }

    private static LivingEntity findEntityById(UUID entityId) {
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            // Get the server world and find entity by UUID
            ServerLevel serverLevel = Minecraft.getInstance().getSingleplayerServer().getLevel(Level.OVERWORLD);
            if (serverLevel != null) {
                Entity entity = serverLevel.getEntity(entityId);
                if (entity instanceof LivingEntity livingEntity) {
                    return livingEntity;
                }
            }
        }
        return null;
    }

    private static ServerPlayer getServerPlayer(UUID playerId) {
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayer(playerId);
        }
        return null;
    }

    private static Fighter getPrimaryFighterClass(Player player) {
        var cap = player.getCapability(net.pablo.rpgclasses.capability.PlayerClassProvider.PLAYER_CLASS_CAPABILITY).orElse(null);
        if (cap == null) return null;
        if (cap.getSelectedClass() instanceof Fighter fPrimary) return fPrimary;
        return null;
    }
}