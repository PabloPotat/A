package net.pablo.rpgclasses.effects;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

// CustomStunEffect.java
public class CustomStunEffect extends MobEffect {
    public CustomStunEffect() {
        super(MobEffectCategory.HARMFUL, 0xADD8E6); // Light blue color
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Completely stop all movement
        entity.setDeltaMovement(0, 0, 0);

        // Prevent jumping and reduce fall distance
        entity.fallDistance = 0;

        // For players, disable movement abilities
        if (entity instanceof Player player) {
            player.getAbilities().flying = false;
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply every tick to ensure continuous freeze
        return true;
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}