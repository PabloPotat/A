package net.pablo.rpgclasses;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.pablo.rpgclasses.skills.SkybreakerLandingPacket;
import net.pablo.rpgclasses.skills.SkybreakerStrikePacket;

// NetworkHandler.java
public class NetworkHandler {
    public static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation("rpgclasses", "main"))
            .networkProtocolVersion(() -> "1.0")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();

    private static int packetId = 0;

    public static void register() {
        INSTANCE.messageBuilder(SkybreakerStrikePacket.class, packetId++)
                .encoder(SkybreakerStrikePacket::encode)
                .decoder(SkybreakerStrikePacket::decode)
                .consumerMainThread(SkybreakerStrikePacket::handle)
                .add();

        INSTANCE.messageBuilder(SkybreakerLandingPacket.class, packetId++)
                .encoder(SkybreakerLandingPacket::encode)
                .decoder(SkybreakerLandingPacket::decode)
                .consumerMainThread(SkybreakerLandingPacket::handle)
                .add();
    }
}
