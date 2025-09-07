package net.pablo.rpgclasses;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.pablo.rpgclasses.item.ModCreativeModeTabs;
import net.pablo.rpgclasses.item.ModItems;
import net.pablo.rpgclasses.keybinds.KeyBindings;
import net.pablo.rpgclasses.skills.SkybreakerStrikeHandler;
import net.pablo.rpgclasses.skills.SkybreakerStrikeManager;

@Mod(RpgClassesMod.MOD_ID)
public class RpgClassesMod {
    public static final String MOD_ID = "rpgclasses";

    public RpgClassesMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModCreativeModeTabs.register(modEventBus);

        modEventBus.addListener(this::setup);

        // Register network channel
        NetworkHandler.register();

        // Register items
        ModItems.ITEMS.register(modEventBus);
        // Initialization (optional)
    }
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // In your main mod class setup
    // In your main mod class setup
    private void setup(final FMLCommonSetupEvent event) {
        // Register keybindings
        MinecraftForge.EVENT_BUS.addListener(KeyBindings::register);

        // Register event handlers
        MinecraftForge.EVENT_BUS.addListener(SkybreakerStrikeHandler::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(SkybreakerStrikeManager::onServerTick);

        // Register network
        NetworkHandler.register();
    }
}
