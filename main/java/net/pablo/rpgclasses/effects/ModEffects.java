package net.pablo.rpgclasses.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// ModEffects.java
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "rpgclasses");

    public static final RegistryObject<MobEffect> STUN = EFFECTS.register("stun",
            () -> new CustomStunEffect());

    // Register in your main mod class constructor:
    // EFFECTS.register(modEventBus);
}
