// SkybreakerStrike.java (updated with proper imports)
package net.pablo.rpgclasses.skills;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.pablo.rpgclasses.classes.Fighter;

import java.util.*;

// SkybreakerStrike.java
public class SkybreakerStrike {

    private static final double BASE_DAMAGE = 5.0;
    private static final double BASE_STUN_DURATION = 2.0;
    private static final double STUN_PER_COMBO = 0.5;
    private static final double MAX_STUN_DURATION = 4.0;
    private static final double AOE_RADIUS = 5.0;
    private static final double BASE_ATTACK_SCALING = 0.1;
    private static final double COOLDOWN_REFUND_PERCENT = 0.5;
    private static final double SKILL_COOLDOWN = 10.0;

    // Cooldown per player
    private final Map<UUID, Double> cooldownMap = new HashMap<>();

    // Track players who are currently in skybreaker jump
    private final Map<UUID, Boolean> activeJumpers = new HashMap<>();

    // Track previous ground state for landing detection
    private final Map<UUID, Boolean> previousGroundState = new HashMap<>();

    // Call every server tick
    public void tick() {
        double deltaSeconds = 1.0 / 20.0;
        cooldownMap.replaceAll((key, remaining) -> Math.max(remaining - deltaSeconds, 0.0));

        // Server-side landing detection for all active jumpers
        for (UUID playerId : new ArrayList<>(activeJumpers.keySet())) {
            ServerPlayer player = getServerPlayerByUUID(playerId);
            if (player != null) {
                checkLanding(player);
            } else {
                // Player not found, remove from tracking
                activeJumpers.remove(playerId);
                previousGroundState.remove(playerId);
            }
        }
    }

    private ServerPlayer getServerPlayerByUUID(UUID playerId) {
        // This method needs to access the server's player list
        // You might need to adjust this based on your mod setup
        if (Minecraft.getInstance().getSingleplayerServer() != null) {
            return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayer(playerId);
        }
        return null;
    }

    // Main skill activation
    public void use(Player player) {
        Fighter fighter = getPrimaryFighterClass(player);
        if (fighter == null) {
            System.out.println("No fighter class found for player: " + player.getName().getString());
            return;
        }

        if (getCooldown(player) > 0) {
            System.out.println("Skill on cooldown for player: " + player.getName().getString());
            return;
        }

        System.out.println("Skybreaker Strike started for player: " + player.getName().getString());

        // Start the jump sequence
        activeJumpers.put(player.getUUID(), true);
        previousGroundState.put(player.getUUID(), true); // Player was on ground when they jumped

        // Launch player upward (server-side)
        player.setDeltaMovement(player.getDeltaMovement().x, 0.8, player.getDeltaMovement().z);

        // Play jump sound on server
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.8f);

        // Set cooldown when jump starts
        setCooldown(player, SKILL_COOLDOWN);
    }

    // Server-side landing detection
    private void checkLanding(Player player) {
        UUID playerId = player.getUUID();

        boolean currentlyOnGround = player.onGround();
        boolean previouslyOnGround = previousGroundState.getOrDefault(playerId, false);

        System.out.println("Checking landing for " + player.getName().getString() +
                ": previously=" + previouslyOnGround + ", currently=" + currentlyOnGround);

        // If player was in air and now is on ground, they landed
        if (!previouslyOnGround && currentlyOnGround) {
            System.out.println("Player landed! Dealing damage.");
            // Player landed - deal damage!
            handleLandingDamage(player);
            activeJumpers.remove(playerId);
            previousGroundState.remove(playerId);
        }

        // Update ground state for next tick
        previousGroundState.put(playerId, currentlyOnGround);
    }

    private void handleLandingDamage(Player player) {
        Fighter fighter = getPrimaryFighterClass(player);
        if (fighter == null) {
            System.out.println("No fighter class found for damage calculation");
            return;
        }

        int combo = fighter.getCombo(player.getUUID());
        double damage = calculateDamage(player, fighter, combo);

        Level world = player.level();
        List<LivingEntity> targets = getNearbyEnemies(player, AOE_RADIUS);
        boolean hitAny = false;

        System.out.println("Found " + targets.size() + " targets for damage");

        // Create landing effect
        createLandingEffect(player);

        for (LivingEntity target : targets) {
            System.out.println("Damaging target: " + target.getName().getString());
            target.hurt(player.damageSources().playerAttack(player), (float) damage);

            // Apply STUN effect (cannot move)
            double stunSeconds = calculateStunDuration(combo);
            applyStunEffect(target, (float) stunSeconds);

            hitAny = true;
        }

        if (!hitAny) {
            System.out.println("No targets hit, refunding cooldown");
            refundCooldown(player);
        } else {
            System.out.println("Targets hit, cooldown remains");
        }

        // Play impact sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private double calculateStunDuration(int combo) {
        double stunDuration = BASE_STUN_DURATION + (STUN_PER_COMBO * combo);
        return Math.min(stunDuration, MAX_STUN_DURATION);
    }

    private void applyStunEffect(LivingEntity target, float stunSeconds) {
        System.out.println("Applying stun to " + target.getName().getString() + " for " + stunSeconds + " seconds");

        // Complete stun - cannot move, attack, or use items
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int)(stunSeconds * 20), 255, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, (int)(stunSeconds * 20), 255, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int)(stunSeconds * 20), 255, false, true));

        // Visual stun effect
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int)(stunSeconds * 20), 0, false, false));
    }

    private void createLandingEffect(Player player) {
        Level world = player.level();

        // Create ground impact particles
        for (int i = 0; i < 20; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = world.random.nextDouble() * AOE_RADIUS;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;

            world.addParticle(ParticleTypes.CLOUD, x, player.getY(), z, 0, 0.1, 0);
            world.addParticle(ParticleTypes.CRIT, x, player.getY() + 0.1, z, 0, 0, 0);
        }

        // Stun effect particles around hit enemies
        List<LivingEntity> targets = getNearbyEnemies(player, AOE_RADIUS);
        for (LivingEntity target : targets) {
            for (int i = 0; i < 5; i++) {
                world.addParticle(ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        0, 0.1, 0);
            }
        }
    }

    // ===== Helper Methods =====
    private Fighter getPrimaryFighterClass(Player player) {
        var cap = player.getCapability(net.pablo.rpgclasses.capability.PlayerClassProvider.PLAYER_CLASS_CAPABILITY).orElse(null);
        if (cap == null) return null;
        if (cap.getSelectedClass() instanceof Fighter fPrimary) return fPrimary;
        return null;
    }

    private double calculateDamage(Player player, Fighter fighter, int combo) {
        return BASE_DAMAGE + combo + fighter.getBaseAttack() * BASE_ATTACK_SCALING;
    }

    private List<LivingEntity> getNearbyEnemies(Player player, double radius) {
        AABB area = player.getBoundingBox().inflate(radius);
        return player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
    }

    private UUID getKey(Player player) {
        return player.getUUID();
    }

    public double getCooldown(Player player) {
        return cooldownMap.getOrDefault(getKey(player), 0.0);
    }

    private void setCooldown(Player player, double cooldownSeconds) {
        cooldownMap.put(getKey(player), cooldownSeconds);
    }

    private void refundCooldown(Player player) {
        UUID key = getKey(player);
        double remaining = cooldownMap.getOrDefault(key, 0.0);
        cooldownMap.put(key, Math.max(remaining * (1.0 - COOLDOWN_REFUND_PERCENT), 0.0));
    }
}